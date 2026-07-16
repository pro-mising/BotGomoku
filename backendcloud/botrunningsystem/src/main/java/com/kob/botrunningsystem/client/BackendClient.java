package com.kob.botrunningsystem.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "backend-service")
public interface BackendClient {
    @PostMapping(value = "/pk/receive/bot/move/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String receiveBotMove(@RequestBody MultiValueMap<String, String> data);
}
