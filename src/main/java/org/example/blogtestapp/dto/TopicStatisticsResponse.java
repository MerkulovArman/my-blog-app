package org.example.blogtestapp.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO для статистики по темам (тегам)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicStatisticsResponse {
    private String topic;
    private Long postsCount;
    private Long totalViews;
    private Double averageViews;
}