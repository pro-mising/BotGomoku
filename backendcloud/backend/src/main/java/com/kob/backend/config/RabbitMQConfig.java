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
    public static final String RECORD_EXCHANGE = "botgomoku.record.exchange";
    public static final String RECORD_ANALYSIS_QUEUE = "botgomoku.record.analysis.queue";
    public static final String RECORD_ANALYSIS_ROUTING_KEY = "botgomoku.record.analysis";
    public static final String RANKLIST_EXCHANGE = "botgomoku.ranklist.exchange";
    public static final String RANKLIST_REFRESH_QUEUE = "botgomoku.ranklist.refresh.queue";
    public static final String RANKLIST_REFRESH_ROUTING_KEY = "botgomoku.ranklist.refresh";

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
    public DirectExchange recordExchange() {
        return new DirectExchange(RECORD_EXCHANGE, true, false);
    }

    @Bean
    public Queue recordAnalysisQueue() {
        return new Queue(RECORD_ANALYSIS_QUEUE, true);
    }

    @Bean
    public Binding recordAnalysisBinding(Queue recordAnalysisQueue, DirectExchange recordExchange) {
        return BindingBuilder.bind(recordAnalysisQueue).to(recordExchange).with(RECORD_ANALYSIS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange ranklistExchange() {
        return new DirectExchange(RANKLIST_EXCHANGE, true, false);
    }

    @Bean
    public Queue ranklistRefreshQueue() {
        return new Queue(RANKLIST_REFRESH_QUEUE, true);
    }

    @Bean
    public Binding ranklistRefreshBinding(Queue ranklistRefreshQueue, DirectExchange ranklistExchange) {
        return BindingBuilder.bind(ranklistRefreshQueue).to(ranklistExchange).with(RANKLIST_REFRESH_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
