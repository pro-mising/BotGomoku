package com.kob.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "bot-running-service")
public interface BotRunningClient {
    @PostMapping(value = "/bot/add/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String addBot(@RequestParam("user_id") String userId,
                  @RequestParam("bot_code") String botCode,
                  @RequestParam("input") String input);
}
