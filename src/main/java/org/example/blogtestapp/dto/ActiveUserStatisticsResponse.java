package org.example.blogtestapp.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO для статистики активных пользователей
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveUserStatisticsResponse {
    private String username;
    private String displayName;
    private Long postsCount;
    private Long commentsCount;
    private Long likesReceived;
    private Long totalViews;
    private Double activityScore;
}