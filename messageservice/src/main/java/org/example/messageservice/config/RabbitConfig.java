package org.example.messageservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "message.exchange";
    public static final String QUEUE = "message.published.queue";
    public static final String ROUTING_KEY = "message.published";

    @Bean
    public TopicExchange messageExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue messagePublishedQueue() {
        return new Queue(QUEUE);
    }

    @Bean
    public Binding messagePublishedBinding(Queue messagePublishedQueue, TopicExchange messageExchange) {
        return BindingBuilder
                .bind(messagePublishedQueue)
                .to(messageExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
