package com.fiapx.processor.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.base-path:./storage_data}")
    private String basePathStr;

    private Path basePath;

    @PostConstruct
    public void init() {
        this.basePath = Paths.get(basePathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath.resolve("uploads"));
            Files.createDirectories(this.basePath.resolve("outputs"));
            Files.createDirectories(this.basePath.resolve("temp"));
            log.info("Storage local inicializado em: {}", this.basePath);
        } catch (IOException e) {
            log.error("Erro ao inicializar diretórios de storage", e);
            throw new RuntimeException("Não foi possível inicializar diretórios de storage", e);
        }
    }

    @Override
    public Path getAbsolutePath(String relativeFilePath) {
        return this.basePath.resolve(relativeFilePath).normalize();
    }

    @Override
    public Path createTempDirectory(String prefix) throws IOException {
        Path tempBase = this.basePath.resolve("temp");
        return Files.createTempDirectory(tempBase, prefix);
    }

    @Override
    public String saveOutputFile(File file, String destinationFileName) throws IOException {
        Path destination = this.basePath.resolve("outputs").resolve(destinationFileName);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return "outputs/" + destinationFileName;
    }

    @Override
    public boolean exists(String relativeFilePath) {
        Path filePath = this.basePath.resolve(relativeFilePath).normalize();
        return Files.exists(filePath);
    }
}
