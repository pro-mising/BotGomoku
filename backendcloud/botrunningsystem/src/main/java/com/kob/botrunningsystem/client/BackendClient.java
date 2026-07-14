package com.kob.botrunningsystem.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "backend-service")
public interface BackendClient {
    @PostMapping(value = "/pk/receive/bot/move/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String receiveBotMove(@RequestParam("user_id") String userId,
                          @RequestParam("direction") String direction);
}
