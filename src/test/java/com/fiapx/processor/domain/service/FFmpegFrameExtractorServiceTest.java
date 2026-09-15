package com.fiapx.processor.domain.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Máquinas de desenvolvedor e o runner de CI deste projeto não têm o binário do FFmpeg instalado,
 * então os cenários abaixo exercitam principalmente o caminho de fallback do serviço. O teste do
 * fallback com frames pré-existentes só é válido nessa condição, então é pulado automaticamente
 * (via Assumptions) em qualquer ambiente onde o FFmpeg de fato esteja disponível.
 */
class FFmpegFrameExtractorServiceTest {

    private static boolean ffmpegAvailable;

    private final FFmpegFrameExtractorService service = new FFmpegFrameExtractorService();

    @BeforeAll
    static void detectFfmpeg() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            ffmpegAvailable = process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            ffmpegAvailable = false;
        }
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o arquivo de vídeo não existe")
    void shouldThrowWhenVideoFileDoesNotExist(@TempDir Path tempDir) {
        Path missingVideo = tempDir.resolve("nao_existe.mp4");
        Path outputDir = tempDir.resolve("out");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.extractFrames(missingVideo, outputDir));
        assertTrue(ex.getMessage().contains("não existe"));
    }

    @Test
    @DisplayName("Deve retornar frames pré-existentes via fallback quando o FFmpeg não está disponível")
    void shouldReturnExistingFramesViaFallback(@TempDir Path tempDir) throws Exception {
        Assumptions.assumeFalse(ffmpegAvailable, "FFmpeg está instalado neste ambiente; o fallback não se aplica");

        Path video = Files.createFile(tempDir.resolve("video.mp4"));
        Path outputDir = tempDir.resolve("frames");
        Files.createDirectories(outputDir);
        Files.createFile(outputDir.resolve("frame_0001.png"));
        Files.createFile(outputDir.resolve("frame_0002.png"));

        List<File> frames = service.extractFrames(video, outputDir);

        assertEquals(2, frames.size());
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando o vídeo é inválido e não há frames pré-existentes")
    void shouldThrowWhenVideoInvalidAndNoExistingFrames(@TempDir Path tempDir) throws Exception {
        // Sem FFmpeg: cai no fallback e não encontra frames. Com FFmpeg: o processo falha ao
        // decodificar um arquivo vazio (exit code != 0). Os dois caminhos terminam em RuntimeException.
        Path video = Files.createFile(tempDir.resolve("video.mp4"));
        Path outputDir = tempDir.resolve("frames_vazio");

        assertThrows(RuntimeException.class, () -> service.extractFrames(video, outputDir));
    }
}
