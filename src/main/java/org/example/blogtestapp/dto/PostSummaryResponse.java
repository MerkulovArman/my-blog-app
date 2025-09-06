package org.example.blogtestapp.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO для краткой информации о посте (для списков)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryResponse {
    private Long id;
    private String title;
    private String excerpt; // первые 200 символов content
    private LocalDateTime publishedAt;
    private String authorUsername;
    private Set<String> tagNames;
    private Long likesCount;
    private Long commentsCount;
    private Long viewsCount;
}