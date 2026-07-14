package com.kob.backend.consumer.utils;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread {
    private final Integer rows;
    private final Integer cols;
    private final int[][] g;
    private final Player playerA, playerB;
    private Integer nextStepA = null;
    private Integer nextStepB = null;
    private final ReentrantLock lock = new ReentrantLock();
    private String status = "playing";
    private String loser = "";
    private String finishReason = "";
    private Integer lastMove = -1;
    private final static String addBotUrl = "http://127.0.0.1:3002/bot/add/";
    private final static Integer HUMAN_BOT_ID = -1;

    public Game(Integer rows,
                Integer cols,
                Integer idA,
                Integer botIdA,
                Bot botA,
                Integer idB,
                Integer botIdB,
                Bot botB
    ) {
        this.rows = rows;
        this.cols = cols;
        this.g = new int[rows][cols];

        if (botIdA == null) botIdA = HUMAN_BOT_ID;
        if (botIdB == null) botIdB = HUMAN_BOT_ID;

        String botCodeA = "", botCodeB = "";
        if (botA != null) {
            botIdA = botA.getId();
            botCodeA = botA.getContent();
        }
        if (botB != null) {
            botIdB = botB.getId();
            botCodeB = botB.getContent();
        }

        playerA = new Player(idA, botIdA, botCodeA, 0, 0, new ArrayList<>());
        playerB = new Player(idB, botIdB, botCodeB, 0, 0, new ArrayList<>());
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public int[][] getG() {
        return g;
    }

    public Integer getCurrentPlayerId() {
        return isATurn() ? playerA.getId() : playerB.getId();
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            if (this.nextStepA == null) this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            if (this.nextStepB == null) this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public void createMap() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                g[i][j] = 0;
            }
        }
    }

    private boolean isATurn() {
        return (playerA.getSteps().size() + playerB.getSteps().size()) % 2 == 0;
    }

    private Player currentPlayer() {
        return isATurn() ? playerA : playerB;
    }

    private Player opponentPlayer() {
        return isATurn() ? playerB : playerA;
    }

    private int currentPiece() {
        return isATurn() ? 1 : 2;
    }

    private String getBoardString() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res.append(g[i][j]);
            }
        }
        return res.toString();
    }

    private String getInput(Player player) {
        int piece = playerA.getId().equals(player.getId()) ? 1 : 2;
        return getBoardString() + "#" + piece + "#" + lastMove;
    }

    private void sendBotCode(Player player) {
        if (player.getBotId().equals(HUMAN_BOT_ID)) return;
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", player.getId().toString());
        data.add("bot_code", player.getBotCode());
        data.add("input", getInput(player));
        WebSocketServer.restTemplate.postForObject(addBotUrl, data, String.class);
    }

    private boolean nextStep() {
        Player player = currentPlayer();
        boolean aTurn = isATurn();
        sendBotCode(player);

        for (int i = 0; i < 300; i++) {
            try {
                Thread.sleep(100);
                lock.lock();
                try {
                    Integer action = aTurn ? nextStepA : nextStepB;
                    if (action != null) {
                        player.getSteps().add(action);
                        if (aTurn) nextStepA = null;
                        else nextStepB = null;
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        return false;
    }

    private boolean checkValid(Integer action) {
        if (action == null) return false;
        int row = action / cols;
        int col = action % cols;
        return row >= 0 && row < rows && col >= 0 && col < cols && g[row][col] == 0;
    }

    private boolean isFull() {
        return playerA.getSteps().size() + playerB.getSteps().size() >= rows * cols;
    }

    private boolean hasFive(int row, int col, int piece) {
        int[] dx = {0, 1, 1, 1};
        int[] dy = {1, 0, 1, -1};
        for (int d = 0; d < 4; d++) {
            int count = 1;
            for (int k = 1; ; k++) {
                int x = row + dx[d] * k, y = col + dy[d] * k;
                if (x < 0 || x >= rows || y < 0 || y >= cols || g[x][y] != piece) break;
                count++;
            }
            for (int k = 1; ; k++) {
                int x = row - dx[d] * k, y = col - dy[d] * k;
                if (x < 0 || x >= rows || y < 0 || y >= cols || g[x][y] != piece) break;
                count++;
            }
            if (count >= 5) return true;
        }
        return false;
    }

    private void judge(Integer action, Player player, Player opponent, int piece) {
        if (!checkValid(action)) {
            status = "finished";
            loser = playerA.getId().equals(player.getId()) ? "A" : "B";
            finishReason = "invalid-move";
            return;
        }

        int row = action / cols;
        int col = action % cols;
        g[row][col] = piece;
        lastMove = action;

        if (hasFive(row, col, piece)) {
            status = "finished";
            loser = playerA.getId().equals(opponent.getId()) ? "A" : "B";
            finishReason = "five-in-a-row";
        } else if (isFull()) {
            status = "finished";
            loser = "all";
            finishReason = "draw";
        }
    }

    private void sendAllMessage(String message) {
        if (WebSocketServer.users.get(playerA.getId()) != null)
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        if (WebSocketServer.users.get(playerB.getId()) != null)
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
    }

    private void sendMove(Integer action, int piece) {
        JSONObject resp = new JSONObject();
        resp.put("event", "move");
        resp.put("action", action);
        resp.put("row", action / cols);
        resp.put("col", action % cols);
        resp.put("piece", piece);
        resp.put("next_player", getCurrentPlayerId());
        sendAllMessage(resp.toJSONString());
    }

    private void updateUserRating(Player player, Integer rating) {
        User user = WebSocketServer.userMapper.selectById(player.getId());
        user.setRating(rating);
        WebSocketServer.userMapper.updateById(user);
    }

    private void saveToDatabase() {
        Integer ratingA = WebSocketServer.userMapper.selectById(playerA.getId()).getRating();
        Integer ratingB = WebSocketServer.userMapper.selectById(playerB.getId()).getRating();

        if ("A".equals(loser)) {
            ratingA -= 2;
            ratingB += 5;
        } else if ("B".equals(loser)) {
            ratingA += 5;
            ratingB -= 2;
        }

        updateUserRating(playerA, ratingA);
        updateUserRating(playerB, ratingB);

        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                loser,
                "",
                new Date()
        );

        WebSocketServer.recordMapper.insert(record);
    }

    private void sendResult() {
        JSONObject resp = new JSONObject();
        resp.put("event", "result");
        resp.put("loser", loser);
        resp.put("winner", "A".equals(loser) ? "B" : ("B".equals(loser) ? "A" : "all"));
        resp.put("reason", finishReason);
        resp.put("rule", "horizontal_vertical_diagonal_five");
        resp.put("map", g);
        sendAllMessage(resp.toJSONString());
        try {
            saveToDatabase();
        } catch (Exception e) {
            System.err.println("save record failed: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while ("playing".equals(status)) {
            Player player = currentPlayer();
            Player opponent = opponentPlayer();
            int piece = currentPiece();

            if (nextStep()) {
                List<Integer> steps = player.getSteps();
                Integer action = steps.get(steps.size() - 1);
                boolean validAction = checkValid(action);
                judge(action, player, opponent, piece);

                if ("playing".equals(status)) {
                    sendMove(action, piece);
                } else {
                    if (validAction) sendMove(action, piece);
                    sendResult();
                }
            } else {
                status = "finished";
                loser = playerA.getId().equals(player.getId()) ? "A" : "B";
                finishReason = "timeout";
                sendResult();
            }
        }
    }
}
