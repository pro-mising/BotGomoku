package com.kob.backend.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.client.BotRunningClient;
import com.kob.backend.client.MatchingClient;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.RedisCacheService;
import com.kob.backend.service.impl.record.RecordAnalysisService;
import com.kob.backend.service.ranklist.GetRanklistService;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/websocket/{token}")
public class WebSocketServer {
    public static final ConcurrentHashMap<Integer, WebSocketServer> users = new ConcurrentHashMap<>();

    private User user;
    private Session session = null;

    public static RecordMapper recordMapper;
    public static UserMapper userMapper;
    private static BotMapper botMapper;
    public static BotRunningClient botRunningClient;
    private static MatchingClient matchingClient;
    private static RedisCacheService redisCacheService;
    public static RecordAnalysisService recordAnalysisService;
    public static GetRanklistService ranklistService;

    public Game game = null;

    public static boolean isUserConnected(Integer userId) {
        return userId != null && users.containsKey(userId);
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    @Autowired
    public void setBotMapper(BotMapper botMapper) {
        WebSocketServer.botMapper = botMapper;
    }

    @Autowired
    public void setRecordMapper(RecordMapper recordMapper) {
        WebSocketServer.recordMapper = recordMapper;
    }

    @Autowired
    public void setMatchingClient(MatchingClient matchingClient) {
        WebSocketServer.matchingClient = matchingClient;
    }

    @Autowired
    public void setBotRunningClient(BotRunningClient botRunningClient) {
        WebSocketServer.botRunningClient = botRunningClient;
    }

    @Autowired
    public void setRedisCacheService(RedisCacheService redisCacheService) {
        WebSocketServer.redisCacheService = redisCacheService;
    }

    @Autowired
    public void setRecordAnalysisService(RecordAnalysisService recordAnalysisService) {
        WebSocketServer.recordAnalysisService = recordAnalysisService;
    }

    @Autowired
    public void setRanklistService(GetRanklistService ranklistService) {
        WebSocketServer.ranklistService = ranklistService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;

        Integer userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);

        if (this.user != null) {
            users.put(userId, this);
            if (redisCacheService != null) redisCacheService.markOnline(this.user);
            System.out.println("connected");
        } else {
            System.out.println("not connected");
            try {
                this.session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClose
    public void onClose() {
        System.out.println("disconnected");
        if (this.user != null) {
            users.remove(this.user.getId());
            if (redisCacheService != null) redisCacheService.markOffline(this.user.getId());
        }
    }

    public static void startGame(Integer aId, Integer aBotId, Integer bId, Integer bBotId) {
        User a = userMapper.selectById(aId);
        Bot botA = aBotId != null && aBotId > 0 ? botMapper.selectById(aBotId) : null;
        User b = userMapper.selectById(bId);
        Bot botB = bBotId != null && bBotId > 0 ? botMapper.selectById(bBotId) : null;

        Game game = new Game(15, 15, a.getId(), aBotId, botA, b.getId(), bBotId, botB);
        game.createMap();

        if (users.get(a.getId()) != null) users.get(a.getId()).game = game;
        if (users.get(b.getId()) != null) users.get(b.getId()).game = game;

        JSONObject respGame = new JSONObject();
        respGame.put("a_id", game.getPlayerA().getId());
        respGame.put("a_sx", game.getPlayerA().getSx());
        respGame.put("a_sy", game.getPlayerA().getSy());
        respGame.put("b_id", game.getPlayerB().getId());
        respGame.put("b_sx", game.getPlayerB().getSx());
        respGame.put("b_sy", game.getPlayerB().getSy());
        respGame.put("map", game.getG());
        respGame.put("current_player", game.getCurrentPlayerId());

        JSONObject respA = new JSONObject();
        respA.put("event", "start-matching");
        respA.put("opponent_username", b.getUsername());
        respA.put("opponent_photo", b.getPhoto());
        respA.put("game", respGame);
        if (users.get(a.getId()) != null) users.get(a.getId()).sendMessage(respA.toJSONString());

        JSONObject respB = new JSONObject();
        respB.put("event", "start-matching");
        respB.put("opponent_username", a.getUsername());
        respB.put("opponent_photo", a.getPhoto());
        respB.put("game", respGame);
        if (users.get(b.getId()) != null) users.get(b.getId()).sendMessage(respB.toJSONString());

        game.start();
    }

    private void startMatching(Integer botId) {
        matchingClient.addPlayer(this.user.getId().toString(), this.user.getRating().toString(), botId.toString());
    }

    private void stopMatching() {
        matchingClient.removePlayer(this.user.getId().toString());
    }

    private void move(Integer direction) {
        if (direction == null || game == null || !user.getId().equals(game.getCurrentPlayerId())) return;

        if (game.getPlayerA().getId().equals(user.getId())) {
            if (game.getPlayerA().getBotId().equals(-1)) game.setNextStepA(direction);
        } else if (game.getPlayerB().getId().equals(user.getId())) {
            if (game.getPlayerB().getBotId().equals(-1)) game.setNextStepB(direction);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject data = JSONObject.parseObject(message);
        String event = data.getString("event");
        if ("start-matching".equals(event)) {
            startMatching(data.getInteger("bot_id"));
        } else if ("stop-matching".equals(event)) {
            stopMatching();
        } else if ("move".equals(event)) {
            move(data.getInteger("direction"));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
