package com.kob.botrunningsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean //想取得谁就加一个Bean注解
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

}
