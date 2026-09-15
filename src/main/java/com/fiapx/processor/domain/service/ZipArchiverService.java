package com.fiapx.processor.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ZipArchiverService {

    public File createZipArchive(List<File> filesToZip, Path outputZipPath) throws IOException {
        log.info("Iniciando compactação de {} arquivos em: {}", filesToZip.size(), outputZipPath);

        File zipFile = outputZipPath.toFile();
        if (zipFile.getParentFile() != null) {
            zipFile.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            byte[] buffer = new byte[8192];

            for (File file : filesToZip) {
                if (!file.exists() || !file.isFile()) {
                    continue;
                }

                ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);

                try (FileInputStream fis = new FileInputStream(file)) {
                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }

        log.info("Arquivo ZIP gerado com sucesso: {} (Tamanho: {} bytes)", zipFile.getName(), zipFile.length());
        return zipFile;
    }
}
