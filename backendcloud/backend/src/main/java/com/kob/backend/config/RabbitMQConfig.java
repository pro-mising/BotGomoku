package com.kob.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String BOT_EVALUATION_EXCHANGE = "bot.evaluation.exchange";
    public static final String DEEPSEEK_QUEUE = "bot.evaluation.deepseek.queue";
    public static final String DEEPSEEK_ROUTING_KEY = "bot.evaluation.deepseek";

    @Bean
    public DirectExchange botEvaluationExchange() {
        return new DirectExchange(BOT_EVALUATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue deepSeekQueue() {
        return new Queue(DEEPSEEK_QUEUE, true);
    }

    @Bean
    public Binding deepSeekBinding(Queue deepSeekQueue, DirectExchange botEvaluationExchange) {
        return BindingBuilder.bind(deepSeekQueue).to(botEvaluationExchange).with(DEEPSEEK_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
