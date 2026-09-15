package com.fiapx.processor.infrastructure.messaging;

import com.fiapx.processor.domain.service.VideoProcessorUseCase;
import com.fiapx.processor.infrastructure.messaging.dto.VideoProcessEvent;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class VideoProcessConsumerTest {

    private VideoProcessorUseCase videoProcessorUseCase;
    private VideoProcessConsumer consumer;
    private Channel channel;

    @BeforeEach
    void setUp() {
        videoProcessorUseCase = mock(VideoProcessorUseCase.class);
        consumer = new VideoProcessConsumer(videoProcessorUseCase);
        channel = mock(Channel.class);
    }

    @Test
    @DisplayName("Deve confirmar (ACK) a mensagem quando o processamento é bem-sucedido")
    void shouldAckWhenProcessingSucceeds() throws Exception {
        VideoProcessEvent event = VideoProcessEvent.builder().videoId(1L).build();

        consumer.consumeVideoProcessEvent(event, channel, 42L);

        verify(videoProcessorUseCase).processVideo(event);
        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Deve rejeitar (NACK) a mensagem sem requeue quando o processamento falha")
    void shouldNackWhenProcessingFails() throws Exception {
        VideoProcessEvent event = VideoProcessEvent.builder().videoId(1L).build();
        doThrow(new RuntimeException("falha no processamento")).when(videoProcessorUseCase).processVideo(event);

        consumer.consumeVideoProcessEvent(event, channel, 42L);

        verify(channel).basicNack(42L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
