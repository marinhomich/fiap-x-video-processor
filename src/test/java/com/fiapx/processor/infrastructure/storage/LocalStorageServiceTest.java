package com.fiapx.processor.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService();
        ReflectionTestUtils.setField(storageService, "basePathStr", tempDir.toString());
        storageService.init();
    }

    @Test
    @DisplayName("init deve criar os diretórios uploads, outputs e temp")
    void shouldCreateBaseDirectoriesOnInit() {
        assertTrue(Files.isDirectory(tempDir.resolve("uploads")));
        assertTrue(Files.isDirectory(tempDir.resolve("outputs")));
        assertTrue(Files.isDirectory(tempDir.resolve("temp")));
    }

    @Test
    @DisplayName("getAbsolutePath deve resolver o caminho relativo a partir do base path")
    void shouldResolveAbsolutePath() {
        Path resolved = storageService.getAbsolutePath("uploads/video.mp4");

        assertEquals(tempDir.resolve("uploads/video.mp4").normalize(), resolved);
    }

    @Test
    @DisplayName("createTempDirectory deve criar um diretório dentro de temp/")
    void shouldCreateTempDirectory() throws Exception {
        Path created = storageService.createTempDirectory("video_1_");

        assertTrue(Files.isDirectory(created));
        assertTrue(created.startsWith(tempDir.resolve("temp")));
    }

    @Test
    @DisplayName("saveOutputFile deve copiar o arquivo para outputs/ e retornar o caminho relativo")
    void shouldSaveOutputFile() throws Exception {
        File source = Files.createTempFile(tempDir, "frames", ".zip").toFile();
        Files.writeString(source.toPath(), "conteudo-zip");

        String relativePath = storageService.saveOutputFile(source, "frames_video_1.zip");

        assertEquals("outputs/frames_video_1.zip", relativePath);
        assertTrue(Files.exists(tempDir.resolve("outputs").resolve("frames_video_1.zip")));
    }

    @Test
    @DisplayName("exists deve retornar true/false corretamente")
    void shouldCheckExistence() throws Exception {
        assertFalse(storageService.exists("outputs/nao_existe.zip"));

        Files.writeString(tempDir.resolve("outputs").resolve("existe.zip"), "conteudo");

        assertTrue(storageService.exists("outputs/existe.zip"));
    }
}
