package com.kob.matchingsystem.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {
    @Bean
    public RequestInterceptor internalTokenInterceptor(@Value("${kob.internal-token}") String internalToken) {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}
