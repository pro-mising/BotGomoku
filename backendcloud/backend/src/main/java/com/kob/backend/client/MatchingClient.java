package com.kob.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "matching-service")
public interface MatchingClient {
    @PostMapping(value = "/player/add/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String addPlayer(@RequestParam("user_id") String userId,
                     @RequestParam("rating") String rating,
                     @RequestParam("bot_id") String botId);

    @PostMapping(value = "/player/remove/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String removePlayer(@RequestParam("user_id") String userId);
}
