package org.example.blogtestapp.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO для ответа с информацией о посте
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private Boolean isPublished;
    private Long viewsCount;
    private String authorUsername;
    private Long authorId;
    private Set<String> tagNames;
    private Long likesCount;
    private Long commentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}