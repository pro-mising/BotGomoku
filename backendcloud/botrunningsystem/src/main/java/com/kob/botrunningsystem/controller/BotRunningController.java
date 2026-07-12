package com.kob.botrunningsystem.controller;

import com.kob.botrunningsystem.service.BotRunnigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class BotRunningController {

    @Autowired
    private BotRunnigService botRunnigService;

    @PostMapping("/bot/add/")
    public String addBot(@RequestParam MultiValueMap<String, String> data) {
        System.out.println("bot add controller: " + data );

        Integer userId = Integer.parseInt(Objects.requireNonNull(data.getFirst("user_id")));
        System.out.println("bot add controller: " + userId );
        String botCode = data.getFirst("bot_code");
        String input = data.getFirst("input");
        return botRunnigService.addBot(userId, botCode, input);
    }
}
