package com.kob.backend.config;

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

//当要用到某个东西的时候就定义它的一个configuration,然后定义一个注解Bean,然后返回他的一个实体
//ResTemplate可以在两个Springboot进程之间进行通信
