package com.kob.matchingsystem.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "backend-service")
public interface BackendClient {
    @PostMapping(value = "/pk/start/game/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String startGame(@RequestParam("a_id") String aId,
                     @RequestParam("a_bot_id") String aBotId,
                     @RequestParam("b_id") String bId,
                     @RequestParam("b_bot_id") String bBotId);
}
