package com.fiapx.processor.infrastructure.messaging;

import com.fiapx.processor.domain.service.VideoProcessorUseCase;
import com.fiapx.processor.infrastructure.messaging.dto.VideoProcessEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessConsumer {

    private final VideoProcessorUseCase videoProcessorUseCase;

    @RabbitListener(queues = "${rabbitmq.queue.video-process}", containerFactory = "rabbitListenerContainerFactory")
    public void consumeVideoProcessEvent(
            VideoProcessEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("Recebida mensagem da fila video.process.queue para o vídeo ID: {}", event.getVideoId());

        try {
            videoProcessorUseCase.processVideo(event);
            channel.basicAck(deliveryTag, false);
            log.info("Mensagem confirmada (ACK) com sucesso para o vídeo ID: {}", event.getVideoId());
        } catch (Exception e) {
            log.error("Erro no processamento do vídeo ID: {}. Enviando NACK (sem requeue para ir para DLQ se configurado).", event.getVideoId(), e);
            // Rejeita a mensagem e encaminha para DLQ (requeue = false)
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
