package com.kob.backend.service.impl.bot.evaluation;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.bot.builtin.BuiltInBotFactory;
import com.kob.backend.bot.builtin.BuiltInGomokuBot;
import com.kob.backend.bot.builtin.GomokuBotSupport;
import com.kob.backend.config.RabbitMQConfig;
import com.kob.backend.mapper.BotEvaluationReportMapper;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.BotEvaluationReport;
import com.kob.backend.pojo.User;
import com.kob.backend.service.bot.evaluation.BotEvaluationService;
import com.kob.backend.service.impl.utils.RedisCacheService;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class BotEvaluationServiceImpl implements BotEvaluationService {
    private static final int SIZE = 15;
    private static final int EMPTY = 0;
    private static final int BLACK = 1;
    private static final int WHITE = 2;
    private static final int MAX_STEPS = SIZE * SIZE;

    @Autowired
    private BotMapper botMapper;

    @Autowired
    private BotEvaluationReportMapper reportMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisCacheService redisCacheService;

    @Value("${deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String deepSeekApiKey;

    @Value("${deepseek.model:${DEEPSEEK_MODEL:deepseek-chat}}")
    private String deepSeekModel;

    @Value("${deepseek.base-url:${DEEPSEEK_BASE_URL:https://api.deepseek.com}}")
    private String deepSeekBaseUrl;

    @Override
    public JSONObject runEvaluation(Map<String, String> data) {
        JSONObject resp = new JSONObject();
        User user = currentUser();
        Integer botId = Integer.parseInt(data.getOrDefault("bot_id", "-1"));
        String mode = data.getOrDefault("mode", "quick");
        int gamesPerSide = "standard".equals(mode) ? 3 : 1;

        Bot bot = botMapper.selectById(botId);
        if (bot == null || !bot.getUserId().equals(user.getId())) {
            resp.put("error_message", "Bot不存在或无权限");
            return resp;
        }

        UserBotRunner userBotRunner;
        try {
            userBotRunner = new UserBotRunner(bot.getContent());
        } catch (Exception e) {
            resp.put("error_message", "Bot编译失败：" + e.getMessage());
            return resp;
        }

        EvaluationStats stats = new EvaluationStats(bot.getTitle(), mode);
        List<BuiltInGomokuBot> opponents = BuiltInBotFactory.evaluationBots();
        for (BuiltInGomokuBot opponent : opponents) {
            for (int i = 0; i < gamesPerSide; i++) {
                stats.addGame(playGame(userBotRunner, opponent, BLACK));
                stats.addGame(playGame(userBotRunner, opponent, WHITE));
            }
        }

        JSONObject reportJson = stats.toJson();
        BotEvaluationReport savedReport = saveEvaluationReport(user, bot, mode, reportJson);
        redisCacheService.cacheLatestEvaluationReport(user.getId(), savedReport.getId());
        reportJson.put("report_id", savedReport.getId());

        resp.put("error_message", "success");
        resp.put("report_id", savedReport.getId());
        resp.put("report", reportJson);
        return resp;
    }

    @Override
    public JSONObject analyzeWithDeepSeek(Map<String, String> data) {
        JSONObject resp = new JSONObject();
        if (deepSeekApiKey == null || deepSeekApiKey.isBlank()) {
            resp.put("error_message", "请先在后端配置 DEEPSEEK_API_KEY");
            return resp;
        }

        User user = currentUser();
        Integer botId = Integer.parseInt(data.getOrDefault("bot_id", "-1"));
        Bot bot = botMapper.selectById(botId);
        if (bot == null || !bot.getUserId().equals(user.getId())) {
            resp.put("error_message", "Bot不存在或无权限");
            return resp;
        }

        BotEvaluationReport savedReport = findReportForAnalysis(user.getId(), botId, data.get("report_id"));
        if (savedReport == null) {
            resp.put("error_message", "请先完成一次 Bot 测评");
            return resp;
        }

        if ("analyzing".equals(savedReport.getAnalysisStatus())) {
            resp.put("error_message", "success");
            resp.put("report_id", savedReport.getId());
            resp.put("analysis", buildAnalysisJson(savedReport));
            resp.put("optimized_code", savedReport.getOptimizedCode());
            return resp;
        }

        savedReport.setAnalysisStatus("analyzing");
        savedReport.setModifytime(new Date());
        reportMapper.updateById(savedReport);

        publishDeepSeekAnalysisTask(savedReport.getId(), bot.getId());

        resp.put("error_message", "success");
        resp.put("report_id", savedReport.getId());
        resp.put("analysis", buildAnalysisJson(savedReport));
        resp.put("optimized_code", savedReport.getOptimizedCode());
        return resp;
    }

    private void publishDeepSeekAnalysisTask(Integer reportId, Integer botId) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOT_EVALUATION_EXCHANGE,
                    RabbitMQConfig.DEEPSEEK_ROUTING_KEY,
                    new DeepSeekAnalysisMessage(reportId, botId)
            );
        } catch (AmqpException e) {
            Thread fallbackThread = new Thread(() -> runDeepSeekAnalysis(reportId, botId), "deepseek-bot-evaluation-fallback-" + reportId);
            fallbackThread.start();
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DEEPSEEK_QUEUE)
    public void consumeDeepSeekAnalysis(DeepSeekAnalysisMessage message) {
        if (message == null || message.getReportId() == null || message.getBotId() == null) return;
        runDeepSeekAnalysis(message.getReportId(), message.getBotId());
    }

    private void runDeepSeekAnalysis(Integer reportId, Integer botId) {
        BotEvaluationReport savedReport = reportMapper.selectById(reportId);
        if (savedReport == null) return;
        Bot bot = botMapper.selectById(botId);
        if (bot == null || !bot.getUserId().equals(savedReport.getUserId())) {
            savedReport.setAnalysisStatus("failed");
            savedReport.setAnalysisFindings("DeepSeek 分析失败：Bot 不存在或无权限");
            savedReport.setAnalysisWeaknesses("");
            savedReport.setAnalysisSuggestions("请重新完成 Bot 测评后再分析。");
            savedReport.setModifytime(new Date());
            reportMapper.updateById(savedReport);
            return;
        }

        String prompt = buildDeepSeekPrompt(savedReport.getReportJson(), bot.getContent());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekApiKey);

            JSONObject body = new JSONObject();
            body.put("model", deepSeekModel);
            body.put("temperature", 0.2);
            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", "你是五子棋Bot代码评测专家。请用中文输出严格JSON，不要输出Markdown。");
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(system);
            messages.add(userMessage);
            body.put("messages", messages);

            HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
            String resultText = restTemplate.postForObject(
                    deepSeekBaseUrl + "/chat/completions",
                    entity,
                    String.class
            );
            JSONObject result = JSONObject.parseObject(resultText);
            String content = result
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            JSONObject parsed = parseDeepSeekContent(content);
            normalizeAnalysis(parsed);
            savedReport.setAnalysisStatus("success");
            savedReport.setAnalysisFindings(parsed.getString("findings"));
            savedReport.setAnalysisWeaknesses(parsed.getString("weaknesses"));
            savedReport.setAnalysisSuggestions(parsed.getString("suggestions"));
            savedReport.setOptimizedCode(parsed.getString("optimized_code"));
            savedReport.setModifytime(new Date());
            reportMapper.updateById(savedReport);
        } catch (Exception e) {
            savedReport.setAnalysisStatus("failed");
            savedReport.setAnalysisFindings("DeepSeek 分析失败：" + e.getMessage());
            savedReport.setAnalysisWeaknesses("");
            savedReport.setAnalysisSuggestions("请稍后重新点击 DeepSeek赛后分析。");
            savedReport.setModifytime(new Date());
            reportMapper.updateById(savedReport);
        }
    }

    @Override
    public JSONObject getLatestReport(Map<String, String> data) {
        JSONObject resp = new JSONObject();
        User user = currentUser();
        boolean hasBotFilter = data.get("bot_id") != null && !data.get("bot_id").isBlank();
        BotEvaluationReport savedReport = hasBotFilter ? null : findLatestReportFromCache(user.getId());

        if (savedReport == null) {
            QueryWrapper<BotEvaluationReport> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", user.getId());
            if (hasBotFilter) {
                queryWrapper.eq("bot_id", Integer.parseInt(data.get("bot_id")));
            }
            queryWrapper.orderByDesc("createtime").last("limit 1");
            savedReport = reportMapper.selectOne(queryWrapper);
            if (savedReport != null && !hasBotFilter) {
                redisCacheService.cacheLatestEvaluationReport(user.getId(), savedReport.getId());
            }
        }
        if (savedReport == null) {
            resp.put("error_message", "empty");
            return resp;
        }

        JSONObject report = JSONObject.parseObject(savedReport.getReportJson());
        report.put("report_id", savedReport.getId());
        resp.put("error_message", "success");
        resp.put("report_id", savedReport.getId());
        resp.put("bot_id", savedReport.getBotId());
        resp.put("report", report);
        resp.put("analysis", buildAnalysisJson(savedReport));
        resp.put("optimized_code", savedReport.getOptimizedCode());
        return resp;
    }

    private BotEvaluationReport findLatestReportFromCache(Integer userId) {
        Integer reportId = redisCacheService.getLatestEvaluationReportId(userId);
        if (reportId == null) return null;
        BotEvaluationReport report = reportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) return null;
        return report;
    }

    private String buildDeepSeekPrompt(String report, String botCode) {
        return """
                请分析下面这个五子棋 Java Bot 的测评报告和源码。
                你需要返回严格 JSON，格式如下：
                {
                  "findings": "测评发现：概括这个 Bot 的整体水平、胜率、攻防表现和稳定性",
                  "weaknesses": "主要弱点：指出最影响得分的 2 到 4 个问题",
                  "suggestions": "改进建议：给出可执行的优化方向，尽量结合五子棋策略",
                  "optimized_code": "优化后的完整 Java Bot 代码"
                }

                代码要求：
                1. 必须保留 package com.kob.botrunningsystem.utils;
                2. 类名必须是 Bot；
                3. 必须 implements com.kob.botrunningsystem.utils.BotInterface；
                4. 必须实现 public Integer nextMove(String input)；
                5. 返回 0 到 224 的合法位置；
                6. 不要使用外部依赖。
                7. 输出 optimized_code 前必须自行检查 Java 语法、括号、import、类名、方法签名，确保代码可以在当前项目中直接编译通过。
                8. 如果你不确定某个优化能否编译通过，就不要使用该优化，优先返回保守但可编译的完整代码。
                9. optimized_code 只返回完整 Java 源码，不要包含 Markdown 代码围栏。

                测评报告：
                %s

                原始源码：
                %s
                """.formatted(report, botCode);
    }

    private JSONObject parseDeepSeekContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "");
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        }
        try {
            return JSONObject.parseObject(text.trim());
        } catch (Exception e) {
            JSONObject fallback = new JSONObject();
            fallback.put("findings", content);
            fallback.put("weaknesses", "DeepSeek 返回内容不是标准 JSON，系统无法自动拆分弱点。");
            fallback.put("suggestions", "建议重新点击赛后分析，或检查模型输出格式。");
            fallback.put("optimized_code", "");
            return fallback;
        }
    }

    private void normalizeAnalysis(JSONObject parsed) {
        if ((parsed.getString("findings") == null || parsed.getString("findings").isBlank())
                && parsed.getString("analysis") != null) {
            parsed.put("findings", parsed.getString("analysis"));
        }
        if (parsed.getString("findings") == null) parsed.put("findings", "");
        if (parsed.getString("weaknesses") == null) parsed.put("weaknesses", "");
        if (parsed.getString("suggestions") == null) parsed.put("suggestions", "");
        if (parsed.getString("optimized_code") == null) parsed.put("optimized_code", "");
    }

    private BotEvaluationReport saveEvaluationReport(User user, Bot bot, String mode, JSONObject reportJson) {
        Date now = new Date();
        BotEvaluationReport report = new BotEvaluationReport(
                null,
                user.getId(),
                bot.getId(),
                bot.getTitle(),
                mode,
                reportJson.toJSONString(),
                "pending",
                "",
                "",
                "",
                "",
                now,
                now
        );
        reportMapper.insert(report);
        return report;
    }

    private BotEvaluationReport findReportForAnalysis(Integer userId, Integer botId, String reportIdText) {
        if (reportIdText != null && !reportIdText.isBlank()) {
            BotEvaluationReport report = reportMapper.selectById(Integer.parseInt(reportIdText));
            if (report != null && report.getUserId().equals(userId) && report.getBotId().equals(botId)) return report;
            return null;
        }

        QueryWrapper<BotEvaluationReport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("bot_id", botId).orderByDesc("createtime").last("limit 1");
        return reportMapper.selectOne(queryWrapper);
    }

    private JSONObject buildAnalysisJson(BotEvaluationReport savedReport) {
        JSONObject analysis = new JSONObject();
        analysis.put("status", savedReport.getAnalysisStatus());
        analysis.put("findings", savedReport.getAnalysisFindings());
        analysis.put("weaknesses", savedReport.getAnalysisWeaknesses());
        analysis.put("suggestions", savedReport.getAnalysisSuggestions());
        return analysis;
    }

    private GameResult playGame(UserBotRunner userBotRunner, BuiltInGomokuBot opponentBot, int userSide) {
        int[][] board = new int[SIZE][SIZE];
        int currentPlayer = BLACK;
        int lastMove = -1;
        int userMoveCount = 0;
        int attackEvents = 0;
        int defenseChances = 0;
        int defenseSuccess = 0;
        long userCostMs = 0;
        String reason = "draw";
        int winner = 0;
        List<MoveRecord> moves = new ArrayList<>();
        int illegalMoves = 0;
        int timeouts = 0;
        long started = System.currentTimeMillis();

        for (int step = 0; step < MAX_STEPS; step++) {
            int action;
            if (currentPlayer == userSide) {
                boolean shouldBlock = GomokuBotSupport.findImmediateMove(board, GomokuBotSupport.opponent(userSide)) != null;
                if (shouldBlock) defenseChances++;

                long t0 = System.currentTimeMillis();
                UserBotRunner.MoveResult move = userBotRunner.nextMove(getInput(board, userSide, lastMove));
                userCostMs += System.currentTimeMillis() - t0;
                userMoveCount++;

                if (move.timeout) {
                    timeouts++;
                    winner = GomokuBotSupport.opponent(userSide);
                    reason = "timeout";
                    break;
                }
                action = move.action == null ? -1 : move.action;
                if (!valid(board, action)) {
                    illegalMoves++;
                    winner = GomokuBotSupport.opponent(userSide);
                    reason = "invalid-move";
                    break;
                }

                int row = action / SIZE, col = action % SIZE;
                board[row][col] = currentPlayer;
                moves.add(new MoveRecord(action, currentPlayer, true));
                if (shouldBlock && GomokuBotSupport.findImmediateMove(board, GomokuBotSupport.opponent(userSide)) == null) {
                    defenseSuccess++;
                }
                if (GomokuBotSupport.evaluatePoint(board, row, col, userSide) >= 30_000) {
                    attackEvents++;
                }
                if (GomokuBotSupport.hasFive(board, row, col, currentPlayer)) {
                    winner = currentPlayer;
                    reason = "five-in-a-row";
                    break;
                }
                lastMove = action;
            } else {
                action = opponentBot.nextMove(GomokuBotSupport.copy(board), currentPlayer);
                if (!valid(board, action)) {
                    winner = userSide;
                    reason = "opponent-invalid";
                    break;
                }
                int row = action / SIZE, col = action % SIZE;
                board[row][col] = currentPlayer;
                moves.add(new MoveRecord(action, currentPlayer, false));
                if (GomokuBotSupport.hasFive(board, row, col, currentPlayer)) {
                    winner = currentPlayer;
                    reason = "five-in-a-row";
                    break;
                }
                lastMove = action;
            }
            currentPlayer = GomokuBotSupport.opponent(currentPlayer);
        }

        GameResult result = new GameResult();
        result.opponentName = opponentBot.name();
        result.opponentLevel = opponentBot.level();
        result.userSide = userSide == BLACK ? "黑方" : "白方";
        result.winner = winner == 0 ? "平局" : (winner == userSide ? "用户Bot" : opponentBot.name());
        result.win = winner == userSide;
        result.draw = winner == 0;
        result.steps = countPieces(board);
        result.reason = reason;
        result.illegalMoves = illegalMoves;
        result.timeouts = timeouts;
        result.attackEvents = attackEvents;
        result.defenseChances = defenseChances;
        result.defenseSuccess = defenseSuccess;
        result.avgMoveMs = userMoveCount == 0 ? 0 : userCostMs / userMoveCount;
        result.durationMs = System.currentTimeMillis() - started;
        result.moves = moves;
        return result;
    }

    private boolean valid(int[][] board, Integer action) {
        if (action == null) return false;
        int row = action / SIZE, col = action % SIZE;
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE && board[row][col] == EMPTY;
    }

    private String getInput(int[][] board, int player, int lastMove) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                sb.append(board[r][c]);
            }
        }
        return sb + "#" + player + "#" + lastMove;
    }

    private int countPieces(int[][] board) {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != EMPTY) count++;
            }
        }
        return count;
    }

    private User currentUser() {
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        return loginUser.getUser();
    }

    private static class EvaluationStats {
        private final String botName;
        private final String mode;
        private final List<GameResult> games = new ArrayList<>();

        private EvaluationStats(String botName, String mode) {
            this.botName = botName;
            this.mode = mode;
        }

        private void addGame(GameResult result) {
            games.add(result);
        }

        private JSONObject toJson() {
            int wins = 0, blackGames = 0, blackWins = 0, whiteGames = 0, whiteWins = 0;
            int illegal = 0, timeout = 0, attack = 0, defenseChances = 0, defenseSuccess = 0, totalSteps = 0;
            long totalMoveMs = 0;
            JSONArray details = new JSONArray();

            for (GameResult game : games) {
                if (game.win) wins++;
                if ("黑方".equals(game.userSide)) {
                    blackGames++;
                    if (game.win) blackWins++;
                } else {
                    whiteGames++;
                    if (game.win) whiteWins++;
                }
                illegal += game.illegalMoves;
                timeout += game.timeouts;
                attack += game.attackEvents;
                defenseChances += game.defenseChances;
                defenseSuccess += game.defenseSuccess;
                totalSteps += game.steps;
                totalMoveMs += game.avgMoveMs;
                details.add(game.toJson());
            }

            double winRate = percent(wins, games.size());
            double blackWinRate = percent(blackWins, blackGames);
            double whiteWinRate = percent(whiteWins, whiteGames);
            double attackScore = Math.min(100.0, attack * 100.0 / Math.max(1, games.size() * 3));
            double defenseScore = defenseChances == 0 ? 80.0 : percent(defenseSuccess, defenseChances);
            double stabilityScore = Math.max(0.0, 100.0 - illegal * 25.0 - timeout * 35.0);
            double avgSteps = games.isEmpty() ? 0.0 : totalSteps * 1.0 / games.size();
            double efficiencyScore = Math.max(35.0, 100.0 - Math.max(0, avgSteps - 35) * 1.2);
            double overall = winRate * 0.4 + defenseScore * 0.2 + attackScore * 0.2 + stabilityScore * 0.1 + efficiencyScore * 0.1;

            JSONObject metrics = new JSONObject();
            metrics.put("overall_score", round(overall));
            metrics.put("win_rate", round(winRate));
            metrics.put("black_win_rate", round(blackWinRate));
            metrics.put("white_win_rate", round(whiteWinRate));
            metrics.put("attack_score", round(attackScore));
            metrics.put("defense_score", round(defenseScore));
            metrics.put("stability_score", round(stabilityScore));
            metrics.put("efficiency_score", round(efficiencyScore));
            metrics.put("average_steps", round(avgSteps));
            metrics.put("average_move_ms", games.isEmpty() ? 0 : totalMoveMs / games.size());
            metrics.put("illegal_moves", illegal);
            metrics.put("timeouts", timeout);

            JSONObject report = new JSONObject();
            report.put("bot_name", botName);
            report.put("mode", "standard".equals(mode) ? "标准测评" : "快速测评");
            report.put("total_games", games.size());
            report.put("metrics", metrics);
            report.put("games", details);
            report.put("summary", buildSummary(metrics));
            report.put("ai_report", "DeepSeek 赛后分析暂未开启；当前展示的是系统规则引擎生成的测评报告。");
            return report;
        }

        private String buildSummary(JSONObject metrics) {
            return "本次测评综合评分为 " + metrics.getDouble("overall_score")
                    + "，胜率 " + metrics.getDouble("win_rate") + "%。"
                    + "进攻能力 " + metrics.getDouble("attack_score")
                    + "，防守能力 " + metrics.getDouble("defense_score")
                    + "，稳定性 " + metrics.getDouble("stability_score")
                    + "，效率 " + metrics.getDouble("efficiency_score") + "。";
        }

        private double percent(int a, int b) {
            return b == 0 ? 0.0 : a * 100.0 / b;
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    private static class GameResult {
        private String opponentName;
        private String opponentLevel;
        private String userSide;
        private String winner;
        private boolean win;
        private boolean draw;
        private int steps;
        private String reason;
        private int illegalMoves;
        private int timeouts;
        private int attackEvents;
        private int defenseChances;
        private int defenseSuccess;
        private long avgMoveMs;
        private long durationMs;
        private List<MoveRecord> moves;

        private JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray moveItems = new JSONArray();
            for (int i = 0; i < moves.size(); i++) {
                moveItems.add(moves.get(i).toJson(i + 1));
            }
            json.put("opponent_name", opponentName);
            json.put("opponent_level", opponentLevel);
            json.put("user_side", userSide);
            json.put("winner", winner);
            json.put("result", draw ? "平局" : (win ? "胜利" : "失败"));
            json.put("steps", steps);
            json.put("reason", reason);
            json.put("illegal_moves", illegalMoves);
            json.put("timeouts", timeouts);
            json.put("attack_events", attackEvents);
            json.put("defense_chances", defenseChances);
            json.put("defense_success", defenseSuccess);
            json.put("avg_move_ms", avgMoveMs);
            json.put("duration_ms", durationMs);
            json.put("moves", moveItems);
            return json;
        }
    }

    private static class MoveRecord {
        private final int action;
        private final int piece;
        private final boolean userMove;

        private MoveRecord(int action, int piece, boolean userMove) {
            this.action = action;
            this.piece = piece;
            this.userMove = userMove;
        }

        private JSONObject toJson(int step) {
            JSONObject json = new JSONObject();
            json.put("step", step);
            json.put("action", action);
            json.put("row", action / SIZE);
            json.put("col", action % SIZE);
            json.put("piece", piece);
            json.put("side", piece == BLACK ? "黑方" : "白方");
            json.put("owner", userMove ? "用户Bot" : "系统Bot");
            return json;
        }
    }
}
