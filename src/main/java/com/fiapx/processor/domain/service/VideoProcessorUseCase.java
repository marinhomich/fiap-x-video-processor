package com.fiapx.processor.domain.service;

import com.fiapx.processor.domain.entity.Video;
import com.fiapx.processor.domain.entity.enums.VideoStatus;
import com.fiapx.processor.domain.repository.VideoRepository;
import com.fiapx.processor.infrastructure.messaging.dto.VideoProcessEvent;
import com.fiapx.processor.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessorUseCase {

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final FFmpegFrameExtractorService frameExtractorService;
    private final ZipArchiverService zipArchiverService;
    private final NotificationService notificationService;

    @Transactional
    public void processVideo(VideoProcessEvent event) {
        log.info("Recebida mensagem para processamento do vídeo ID: {}", event.getVideoId());

        Video video = videoRepository.findById(event.getVideoId()).orElse(null);
        if (video == null) {
            log.warn("Vídeo com ID {} não foi encontrado no banco de dados. Abortando.", event.getVideoId());
            return;
        }

        // 1. Atualiza status para PROCESSANDO
        video.setStatus(VideoStatus.PROCESSANDO);
        videoRepository.save(video);
        log.info("Vídeo ID: {} atualizado para status PROCESSANDO", video.getId());

        Path tempDir = null;
        try {
            Path videoPath = storageService.getAbsolutePath(video.getFilePath());
            tempDir = storageService.createTempDirectory("video_" + video.getId() + "_");

            // 2. Extrai frames com FFmpeg
            List<File> extractedFrames = frameExtractorService.extractFrames(videoPath, tempDir);

            // 3. Compacta frames em arquivo .zip
            String zipFileName = String.format("frames_video_%d_%d.zip", video.getId(), System.currentTimeMillis());
            Path tempZipPath = tempDir.resolve(zipFileName);
            File zipFile = zipArchiverService.createZipArchive(extractedFrames, tempZipPath);

            // 4. Salva arquivo ZIP no storage persistente
            String storedZipRelativePath = storageService.saveOutputFile(zipFile, zipFileName);

            // 5. Atualiza status para CONCLUIDO
            video.setStatus(VideoStatus.CONCLUIDO);
            video.setZipPath(storedZipRelativePath);
            video.setFrameCount(extractedFrames.size());
            video.setProcessedAt(LocalDateTime.now());
            video.setErrorMessage(null);
            videoRepository.save(video);

            log.info("Processamento do vídeo ID: {} CONCLUÍDO com sucesso! Total de frames: {}", video.getId(), extractedFrames.size());

        } catch (Exception ex) {
            log.error("Erro durante o processamento do vídeo ID: {}", video.getId(), ex);

            video.setStatus(VideoStatus.ERRO);
            video.setErrorMessage(ex.getMessage());
            video.setProcessedAt(LocalDateTime.now());
            videoRepository.save(video);

            // 6. Notifica falha
            notificationService.notifyProcessingError(video.getId(), event.getUserEmail(), ex.getMessage());

            throw new RuntimeException("Falha no processamento do vídeo: " + ex.getMessage(), ex);

        } finally {
            // Limpeza do diretório temporário
            if (tempDir != null && Files.exists(tempDir)) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (Exception e) {
                    log.warn("Não foi possível limpar pasta temporária: {}", tempDir, e);
                }
            }
        }
    }
}
