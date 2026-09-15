package com.fiapx.processor.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.video}")
    private String exchangeName;

    @Value("${rabbitmq.queue.video-process}")
    private String processQueueName;

    @Value("${rabbitmq.queue.video-dlq}")
    private String dlqQueueName;

    @Value("${rabbitmq.routing-key.video-process}")
    private String processRoutingKey;

    @Value("${rabbitmq.routing-key.video-dlq}")
    private String dlqRoutingKey;

    @Bean
    public DirectExchange videoExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue videoProcessQueue() {
        return QueueBuilder.durable(processQueueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Queue videoDlq() {
        return QueueBuilder.durable(dlqQueueName).build();
    }

    @Bean
    public Binding videoProcessBinding() {
        return BindingBuilder.bind(videoProcessQueue())
                .to(videoExchange())
                .with(processRoutingKey);
    }

    @Bean
    public Binding videoDlqBinding() {
        return BindingBuilder.bind(videoDlq())
                .to(videoExchange())
                .with(dlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        return factory;
    }
}
