package com.kob.botrunningsystem.service.impl.utils;

import com.kob.botrunningsystem.client.BackendClient;
import com.kob.botrunningsystem.utils.BotInterface;
import org.joor.Reflect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Consumer extends Thread {
    private static final Pattern BOT_CLASS_PATTERN = Pattern.compile("\\bclass\\s+Bot\\b");

    private Bot bot;
    private static BackendClient backendClient;

    @Autowired
    public void setBackendClient(BackendClient backendClient) {
        Consumer.backendClient = backendClient;
    }

    public void startTimeout(long timeout, Bot bot) {
        this.bot = bot;
        this.start();

        try {
            this.join(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();
        }
    }

    private String addUid(String code, String uid) {
        if (code == null || uid == null) {
            throw new IllegalArgumentException("参数 code 或 uid 不能为 null");
        }

        Matcher matcher = BOT_CLASS_PATTERN.matcher(code);
        if (!matcher.find()) {
            throw new IllegalArgumentException("未找到 class Bot，无法生成动态 Bot 类名");
        }

        return matcher.replaceFirst("class Bot" + uid);
    }

    private void sendMove(Integer direction) {
        if (backendClient == null) {
            System.err.println("bot move callback failed: BackendClient is null");
            return;
        }
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", bot.getUserId().toString());
        data.add("direction", direction.toString());
        backendClient.receiveBotMove(data);
    }

    @Override
    public void run() {
        try {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            BotInterface botInterface = (BotInterface) Reflect.compile(
                    "com.kob.botrunningsystem.utils.Bot" + uid,
                    addUid(bot.getBotCode(), uid)
            ).create().get();

            Integer direction = botInterface.nextMove(bot.getInput());
            if (direction == null) {
                System.err.println("bot returned null move. userId=" + bot.getUserId());
                return;
            }
            System.out.println("bot move: " + bot.getUserId() + " " + direction);
            sendMove(direction);
        } catch (Throwable e) {
            System.err.println("bot execution failed. userId="
                    + bot.getUserId() + ", error=" + e.getMessage());
        }
    }
}
