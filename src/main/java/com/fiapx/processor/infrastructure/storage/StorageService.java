package com.fiapx.processor.infrastructure.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface StorageService {
    Path getAbsolutePath(String relativeFilePath);
    Path createTempDirectory(String prefix) throws IOException;
    String saveOutputFile(File file, String destinationFileName) throws IOException;
    boolean exists(String relativeFilePath);
}
