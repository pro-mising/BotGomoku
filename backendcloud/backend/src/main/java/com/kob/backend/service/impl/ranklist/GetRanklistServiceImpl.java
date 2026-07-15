package com.kob.backend.service.impl.ranklist;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.BotEvaluationReportMapper;
import com.kob.backend.mapper.CommunityCommentMapper;
import com.kob.backend.mapper.CommunityPostMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.BotEvaluationReport;
import com.kob.backend.pojo.CommunityComment;
import com.kob.backend.pojo.CommunityPost;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import com.kob.backend.service.ranklist.GetRanklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetRanklistServiceImpl implements GetRanklistService {
    private static final int LEGACY_PAGE_SIZE = 3;
    private static final int PAGE_SIZE = 10;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private BotEvaluationReportMapper botEvaluationReportMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private CommunityCommentMapper communityCommentMapper;

    @Override
    public JSONObject getList(Integer page) {
        IPage<User> userIPage = new Page<>(page, LEGACY_PAGE_SIZE);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("rating");
        List<User> users = userMapper.selectPage(userIPage, queryWrapper).getRecords();
        JSONObject resp = new JSONObject();
        for (User user : users) {
            user.setPassword("");
        }
        resp.put("users", users);
        resp.put("users_count", userMapper.selectCount(null));
        return resp;
    }

    @Override
    public JSONObject getMultiList(String type, Integer page) {
        String normalizedType = type == null ? "ladder" : type;
        List<JSONObject> entries = switch (normalizedType) {
            case "bot" -> buildBotEntries();
            case "community" -> buildCommunityEntries();
            default -> buildLadderEntries();
        };
        entries.sort(entryComparator(normalizedType));
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).put("rank", i + 1);
        }

        JSONObject resp = new JSONObject();
        resp.put("type", normalizedType);
        resp.put("entries", paginate(entries, page));
        resp.put("total_count", entries.size());
        resp.put("summary", buildSummary(normalizedType, entries));
        return resp;
    }

    private List<JSONObject> buildLadderEntries() {
        List<User> users = userMapper.selectList(null);
        List<Record> records = recordMapper.selectList(null);
        List<JSONObject> entries = new ArrayList<>();
        for (User user : users) {
            LadderStats stats = calculateLadderStats(user.getId(), records);
            JSONObject item = baseUserJson(user);
            item.put("rating", user.getRating() == null ? 0 : user.getRating());
            item.put("games", stats.games);
            item.put("wins", stats.wins);
            item.put("win_rate", stats.games == 0 ? 0 : round(stats.wins * 100.0 / stats.games));
            item.put("main_value", item.getInteger("rating"));
            item.put("sub_value", item.getDoubleValue("win_rate") + "%胜率");
            returnItemLabel(item, "天梯分");
            entries.add(item);
        }
        return entries;
    }

    private List<JSONObject> buildBotEntries() {
        QueryWrapper<BotEvaluationReport> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("createtime");
        List<BotEvaluationReport> reports = botEvaluationReportMapper.selectList(queryWrapper);
        Map<Integer, JSONObject> bestByBot = new HashMap<>();
        for (BotEvaluationReport report : reports) {
            JSONObject reportJson = parseJson(report.getReportJson());
            JSONObject metrics = reportJson.getJSONObject("metrics");
            if (metrics == null) continue;

            double score = metrics.getDoubleValue("overall_score");
            JSONObject currentBest = bestByBot.get(report.getBotId());
            if (currentBest != null && currentBest.getDoubleValue("main_value") >= score) continue;

            User user = userMapper.selectById(report.getUserId());
            JSONObject item = baseUserJson(user);
            item.put("bot_id", report.getBotId());
            item.put("bot_name", report.getBotName());
            item.put("mode", "standard".equals(report.getMode()) ? "标准测评" : "快速测评");
            item.put("overall_score", score);
            item.put("win_rate", metrics.getDoubleValue("win_rate"));
            item.put("attack_score", metrics.getDoubleValue("attack_score"));
            item.put("defense_score", metrics.getDoubleValue("defense_score"));
            item.put("stability_score", metrics.getDoubleValue("stability_score"));
            item.put("efficiency_score", metrics.getDoubleValue("efficiency_score"));
            item.put("average_steps", metrics.getDoubleValue("average_steps"));
            item.put("evaluated_at", report.getCreatetime());
            item.put("main_value", score);
            item.put("sub_value", metrics.getDoubleValue("win_rate") + "%胜率");
            returnItemLabel(item, "综合评分");
            bestByBot.put(report.getBotId(), item);
        }
        return new ArrayList<>(bestByBot.values());
    }

    private List<JSONObject> buildCommunityEntries() {
        List<User> users = userMapper.selectList(null);
        List<JSONObject> entries = new ArrayList<>();
        for (User user : users) {
            QueryWrapper<CommunityPost> postQuery = new QueryWrapper<>();
            postQuery.eq("user_id", user.getId());
            List<CommunityPost> posts = communityPostMapper.selectList(postQuery);

            int likesReceived = 0;
            int commentsReceived = 0;
            for (CommunityPost post : posts) {
                likesReceived += post.getLikes() == null ? 0 : post.getLikes();
                QueryWrapper<CommunityComment> commentQuery = new QueryWrapper<>();
                commentQuery.eq("post_id", post.getId());
                commentsReceived += communityCommentMapper.selectCount(commentQuery).intValue();
            }

            QueryWrapper<CommunityComment> madeCommentQuery = new QueryWrapper<>();
            madeCommentQuery.eq("user_id", user.getId());
            int commentsMade = communityCommentMapper.selectCount(madeCommentQuery).intValue();
            int contributionScore = posts.size() * 20 + likesReceived * 10 + commentsMade * 5;

            JSONObject item = baseUserJson(user);
            item.put("posts", posts.size());
            item.put("likes_received", likesReceived);
            item.put("comments_received", commentsReceived);
            item.put("comments_made", commentsMade);
            item.put("main_value", contributionScore);
            item.put("sub_value", posts.size() + "篇帖子");
            returnItemLabel(item, "贡献分");
            entries.add(item);
        }
        return entries;
    }

    private LadderStats calculateLadderStats(Integer userId, List<Record> records) {
        LadderStats stats = new LadderStats();
        for (Record record : records) {
            boolean isA = userId.equals(record.getAId());
            boolean isB = userId.equals(record.getBId());
            if (!isA && !isB) continue;
            stats.games++;
            if ((isA && "B".equals(record.getLoser())) || (isB && "A".equals(record.getLoser()))) {
                stats.wins++;
            }
        }
        return stats;
    }

    private Comparator<JSONObject> entryComparator(String type) {
        Comparator<JSONObject> comparator = Comparator.comparingDouble(item -> item.getDoubleValue("main_value"));
        if ("ladder".equals(type)) {
            comparator = Comparator.comparingInt(item -> item.getInteger("main_value"));
        }
        return comparator.reversed().thenComparing(item -> item.getString("username"));
    }

    private List<JSONObject> paginate(List<JSONObject> entries, Integer page) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int from = Math.min((currentPage - 1) * PAGE_SIZE, entries.size());
        int to = Math.min(from + PAGE_SIZE, entries.size());
        return new ArrayList<>(entries.subList(from, to));
    }

    private JSONObject buildSummary(String type, List<JSONObject> entries) {
        JSONObject summary = new JSONObject();
        summary.put("total", entries.size());
        summary.put("leader", entries.isEmpty() ? "" : entries.get(0).getString("username"));
        summary.put("leader_value", entries.isEmpty() ? 0 : entries.get(0).get("main_value"));
        summary.put("label", switch (type) {
            case "bot" -> "Bot强度榜";
            case "community" -> "社区贡献榜";
            default -> "天梯排行榜";
        });
        return summary;
    }

    private JSONObject baseUserJson(User user) {
        JSONObject item = new JSONObject();
        if (user == null) {
            item.put("user_id", 0);
            item.put("username", "Unknown");
            item.put("photo", "");
            return item;
        }
        item.put("user_id", user.getId());
        item.put("username", user.getUsername());
        item.put("photo", user.getPhoto());
        return item;
    }

    private JSONObject parseJson(String text) {
        try {
            return JSONObject.parseObject(text);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private void returnItemLabel(JSONObject item, String label) {
        item.put("main_label", label);
    }

    private static class LadderStats {
        int games;
        int wins;
    }
}
