package com.fiapx.processor.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    @Test
    @DisplayName("Deve registrar notificação de erro sem lançar exceção")
    void shouldLogNotificationWithoutError() {
        assertDoesNotThrow(() -> {
            notificationService.notifyProcessingError(1L, "michel@fiap.com.br", "Vídeo corrompido");
        });
    }
}
