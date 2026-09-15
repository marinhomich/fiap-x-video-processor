package com.fiapx.processor.infrastructure.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessEvent implements Serializable {
    private Long videoId;
    private Long userId;
    private String userEmail;
    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private Long fileSize;
    private LocalDateTime timestamp;
}
