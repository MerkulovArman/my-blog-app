package org.example.blogtestapp.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для информации об аудите постов
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAuditResponse {
    private Long id;
    private Long postId;
    private String operation;
    private LocalDateTime changedAt;
    private Long userId;
    private String oldTitle;
    private String newTitle;
    private String oldContent;
    private String newContent;
    private Boolean oldIsPublished;
    private Boolean newIsPublished;
    private String changeDetails;
}