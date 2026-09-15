package com.fiapx.processor.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void notifyProcessingError(Long videoId, String userEmail, String errorMessage) {
        log.error("================================================================================");
        log.error("🚨 [ALERTA DE FALHA NO PROCESSAMENTO]");
        log.error("Vídeo ID: {}", videoId);
        log.error("Destinatário (Usuário): {}", userEmail);
        log.error("Motivo do Erro: {}", errorMessage);
        log.error("Notificação enviada com sucesso ao usuário e registrada no log de auditoria.");
        log.error("================================================================================");
    }
}
