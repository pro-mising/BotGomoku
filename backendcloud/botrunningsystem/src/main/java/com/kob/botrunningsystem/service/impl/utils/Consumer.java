package com.kob.botrunningsystem.service.impl.utils;

import com.kob.botrunningsystem.utils.BotInterface;
import org.joor.Reflect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class Consumer extends Thread {
    private Bot bot;
    private static RestTemplate restTemplate;
    private final static String receiveBotMoveUrl = "http://127.0.0.1:3000/pk/receive/bot/move/";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        Consumer.restTemplate = restTemplate;
    }

    public void startTimeout(long timeout, Bot bot) {
        this.bot = bot;
        this.start();  //这里start之后就会开启一个新的线程执行后面的这个run函数，并且当前线程会继续执行这个join函数

        try {
            this.join(timeout); //最多等待timeout秒，如果timeout秒执行完了之后前面的线程还没执行，那么就会直接执行下面的代码，如果在timeout秒内线程执行完了，那么就会直接执行下面的代码
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt(); //防止用户程序死循环
        }

    }

    private String addUid(String code, String uid) {
        if (code == null || uid == null) {
            throw new IllegalArgumentException("参数 code 或 uid 不能为 null");
        }

        int k = code.indexOf(" implements com.kob.botrunningsystem.utils.BotInterface");
        if (k == -1) {
//            throw new IllegalArgumentException("未找到接口声明，无法插入 uid");
            System.out.println("未找到接口声明，无法插入 uid");
            return null;
        }

//        System.out.println("我在这里: " + code.substring(0, k) + uid + code.substring(k));
        return code.substring(0, k) + uid + code.substring(k);
    }


    @Override
    public void run() {
        //Reflect 这个函数可以动态编译代码
//        System.out.println("run正在执行");
//        System.out.println("botCode : " + bot.getBotCode());
        UUID uuid = UUID.randomUUID(); //返回结果不一样位就可以，因为比较长
        String uid = uuid.toString().substring(0, 8);//返回前8位，因为太长了
        //在name里面加一个uid，这样可以保证这段代码每次都执行
        BotInterface botInterface = (BotInterface) Reflect.compile(
                "com.kob.botrunningsystem.utils.Bot" + uid,
                addUid(bot.getBotCode(), uid)
        ).create().get();

        Integer direction = botInterface.nextMove(bot.getInput());

        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", bot.getUserId().toString());
        data.add("direction", direction.toString());

        System.out.println("bot move: " + bot.getUserId() + " " + direction);

        restTemplate.postForObject(receiveBotMoveUrl, data, String.class);
    }
}
