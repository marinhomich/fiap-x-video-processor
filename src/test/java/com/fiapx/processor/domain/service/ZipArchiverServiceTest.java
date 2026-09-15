package com.fiapx.processor.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ZipArchiverServiceTest {

    private final ZipArchiverService zipArchiverService = new ZipArchiverService();

    @Test
    @DisplayName("Deve compactar lista de arquivos em um arquivo ZIP válido")
    void shouldCreateZipArchive(@TempDir Path tempDir) throws IOException {
        File file1 = tempDir.resolve("frame_0001.png").toFile();
        File file2 = tempDir.resolve("frame_0002.png").toFile();

        try (FileOutputStream fos1 = new FileOutputStream(file1);
             FileOutputStream fos2 = new FileOutputStream(file2)) {
            fos1.write("dummy image 1".getBytes());
            fos2.write("dummy image 2".getBytes());
        }

        Path outputZipPath = tempDir.resolve("output.zip");
        File zipResult = zipArchiverService.createZipArchive(List.of(file1, file2), outputZipPath);

        assertNotNull(zipResult);
        assertTrue(zipResult.exists());
        assertTrue(zipResult.length() > 0);

        try (ZipFile zipFile = new ZipFile(zipResult)) {
            assertEquals(2, zipFile.size());
            assertNotNull(zipFile.getEntry("frame_0001.png"));
            assertNotNull(zipFile.getEntry("frame_0002.png"));
        }
    }

    @Test
    @DisplayName("Deve ignorar arquivos inexistentes da lista ao compactar")
    void shouldSkipNonExistentFiles(@TempDir Path tempDir) throws IOException {
        File validFile = tempDir.resolve("frame_0001.png").toFile();
        try (FileOutputStream fos = new FileOutputStream(validFile)) {
            fos.write("dummy image".getBytes());
        }
        File missingFile = tempDir.resolve("frame_9999.png").toFile();

        Path outputZipPath = tempDir.resolve("output.zip");
        File zipResult = zipArchiverService.createZipArchive(List.of(validFile, missingFile), outputZipPath);

        try (ZipFile zipFile = new ZipFile(zipResult)) {
            assertEquals(1, zipFile.size());
            assertNotNull(zipFile.getEntry("frame_0001.png"));
        }
    }
}
