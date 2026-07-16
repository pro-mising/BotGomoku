package com.kob.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "bot-running-service")
public interface BotRunningClient {
    @PostMapping(value = "/bot/add/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String addBot(@RequestBody MultiValueMap<String, String> data);
}
