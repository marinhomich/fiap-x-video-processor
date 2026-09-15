package com.fiapx.processor.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "exchangeName", "video.exchange");
        ReflectionTestUtils.setField(config, "processQueueName", "video.process.queue");
        ReflectionTestUtils.setField(config, "dlqQueueName", "video.process.dlq");
        ReflectionTestUtils.setField(config, "processRoutingKey", "video.process.key");
        ReflectionTestUtils.setField(config, "dlqRoutingKey", "video.process.dlq.key");
    }

    @Test
    @DisplayName("Deve criar o exchange durável com o nome configurado")
    void shouldCreateVideoExchange() {
        DirectExchange exchange = config.videoExchange();

        assertEquals("video.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
    }

    @Test
    @DisplayName("Deve criar a fila principal com dead-letter apontando para a exchange e routing key da DLQ")
    void shouldCreateVideoProcessQueueWithDeadLetterArgs() {
        Queue queue = config.videoProcessQueue();

        assertEquals("video.process.queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals("video.exchange", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("video.process.dlq.key", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    @DisplayName("Deve criar a fila de DLQ durável")
    void shouldCreateDlqQueue() {
        Queue dlq = config.videoDlq();

        assertEquals("video.process.dlq", dlq.getName());
        assertTrue(dlq.isDurable());
    }

    @Test
    @DisplayName("Deve vincular a fila principal ao exchange com a routing key de processamento")
    void shouldBindVideoProcessQueue() {
        Binding binding = config.videoProcessBinding();

        assertEquals("video.process.queue", binding.getDestination());
        assertEquals("video.exchange", binding.getExchange());
        assertEquals("video.process.key", binding.getRoutingKey());
    }

    @Test
    @DisplayName("Deve vincular a fila de DLQ ao exchange com a routing key de DLQ")
    void shouldBindDlqQueue() {
        Binding binding = config.videoDlqBinding();

        assertEquals("video.process.dlq", binding.getDestination());
        assertEquals("video.exchange", binding.getExchange());
        assertEquals("video.process.dlq.key", binding.getRoutingKey());
    }

    @Test
    @DisplayName("Deve expor um conversor de mensagens JSON")
    void shouldExposeJsonMessageConverter() {
        MessageConverter converter = config.jsonMessageConverter();

        assertNotNull(converter);
    }

    @Test
    @DisplayName("Deve configurar a container factory com ACK manual e prefetch 1")
    void shouldConfigureListenerContainerFactory() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter converter = config.jsonMessageConverter();

        SimpleRabbitListenerContainerFactory factory = config.rabbitListenerContainerFactory(connectionFactory, converter);

        assertNotNull(factory);
        assertEquals(AcknowledgeMode.MANUAL, ReflectionTestUtils.getField(factory, "acknowledgeMode"));
    }
}
