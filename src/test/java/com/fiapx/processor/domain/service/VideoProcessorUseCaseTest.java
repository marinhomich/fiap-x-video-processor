package com.fiapx.processor.domain.service;

import com.fiapx.processor.domain.entity.Video;
import com.fiapx.processor.domain.entity.enums.VideoStatus;
import com.fiapx.processor.domain.repository.VideoRepository;
import com.fiapx.processor.infrastructure.messaging.dto.VideoProcessEvent;
import com.fiapx.processor.infrastructure.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoProcessorUseCaseTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private FFmpegFrameExtractorService frameExtractorService;

    @Mock
    private ZipArchiverService zipArchiverService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VideoProcessorUseCase videoProcessorUseCase;

    private Video sampleVideo;
    private VideoProcessEvent sampleEvent;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("test_video_proc_");

        sampleVideo = Video.builder()
                .id(1L)
                .originalFileName("video.mp4")
                .storedFileName("stored_video.mp4")
                .filePath("uploads/stored_video.mp4")
                .status(VideoStatus.RECEBIDO)
                .userId(10L)
                .createdAt(LocalDateTime.now())
                .build();

        sampleEvent = VideoProcessEvent.builder()
                .videoId(1L)
                .userId(10L)
                .userEmail("michel@fiap.com.br")
                .filePath("uploads/stored_video.mp4")
                .build();
    }

    @Test
    @DisplayName("Deve processar vídeo com sucesso e atualizar status para CONCLUIDO")
    void shouldProcessVideoSuccessfully() throws Exception {
        File dummyFrame = File.createTempFile("frame_0001", ".png", tempDir.toFile());
        File dummyZip = File.createTempFile("frames_1", ".zip", tempDir.toFile());

        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));
        when(storageService.getAbsolutePath("uploads/stored_video.mp4")).thenReturn(tempDir.resolve("stored_video.mp4"));
        when(storageService.createTempDirectory(any())).thenReturn(tempDir);
        when(frameExtractorService.extractFrames(any(), any())).thenReturn(List.of(dummyFrame));
        when(zipArchiverService.createZipArchive(any(), any())).thenReturn(dummyZip);
        when(storageService.saveOutputFile(eq(dummyZip), any())).thenReturn("outputs/frames_video_1.zip");

        videoProcessorUseCase.processVideo(sampleEvent);

        assertEquals(VideoStatus.CONCLUIDO, sampleVideo.getStatus());
        assertEquals("outputs/frames_video_1.zip", sampleVideo.getZipPath());
        assertEquals(1, sampleVideo.getFrameCount());
        assertNull(sampleVideo.getErrorMessage());
        verify(videoRepository, atLeast(2)).save(sampleVideo);
    }

    @Test
    @DisplayName("Deve atualizar status para ERRO e notificar quando ocorrer falha na extração")
    void shouldHandleErrorAndUpdateStatusToError() throws Exception {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));
        when(storageService.getAbsolutePath("uploads/stored_video.mp4")).thenReturn(tempDir.resolve("stored_video.mp4"));
        when(storageService.createTempDirectory(any())).thenReturn(tempDir);
        when(frameExtractorService.extractFrames(any(), any())).thenThrow(new RuntimeException("Codec inválido"));

        assertThrows(RuntimeException.class, () -> videoProcessorUseCase.processVideo(sampleEvent));

        assertEquals(VideoStatus.ERRO, sampleVideo.getStatus());
        assertEquals("Codec inválido", sampleVideo.getErrorMessage());
        verify(notificationService, times(1)).notifyProcessingError(eq(1L), eq("michel@fiap.com.br"), eq("Codec inválido"));
        verify(videoRepository, atLeast(2)).save(sampleVideo);
    }

    @Test
    @DisplayName("Deve abortar silenciosamente quando o vídeo não é encontrado no banco de dados")
    void shouldAbortWhenVideoNotFound() {
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> videoProcessorUseCase.processVideo(sampleEvent));

        verify(videoRepository, never()).save(any());
        verifyNoInteractions(frameExtractorService, zipArchiverService, notificationService);
    }
}
