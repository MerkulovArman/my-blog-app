package org.example.blogtestapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.dto.PostAuditResponse;
import org.example.blogtestapp.entity.PostAuditLog;
import org.example.blogtestapp.repository.PostAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с аудитом постов
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostAuditService {

    private final PostAuditLogRepository postAuditLogRepository;

    /**
     * Получить историю изменений поста
     */
    public List<PostAuditResponse> getPostAuditHistory(Long postId) {
        log.info("Getting audit history for post ID: {}", postId);
        
        return postAuditLogRepository.findByPostIdOrderByChangedAtDesc(postId)
                .stream()
                .map(this::mapToPostAuditResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить все записи аудита по типу операции
     */
    public List<PostAuditResponse> getAuditByOperation(String operation) {
        return postAuditLogRepository.findByOperationOrderByChangedAtDesc(operation)
                .stream()
                .map(this::mapToPostAuditResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить записи аудита за период
     */
    public List<PostAuditResponse> getAuditByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return postAuditLogRepository.findByChangedAtBetweenOrderByChangedAtDesc(startDate, endDate)
                .stream()
                .map(this::mapToPostAuditResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить последние изменения
     */
    public List<PostAuditResponse> getRecentChanges(int limit) {
        List<PostAuditLog> changes = postAuditLogRepository.findRecentChanges();
        return changes.stream()
                .limit(limit)
                .map(this::mapToPostAuditResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить изменения заголовков
     */
    public List<PostAuditResponse> getTitleChanges() {
        return postAuditLogRepository.findTitleChanges()
                .stream()
                .map(this::mapToPostAuditResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить удаленные посты
     */
    public List<PostAuditResponse> getDeletedPosts() {
        return postAuditLogRepository.findDeletedPosts()
                .stream()
                .map(this::mapToPostAuditResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить статистику изменений поста
     */
    public Long getPostChangeCount(Long postId) {
        return postAuditLogRepository.countChangesByPostId(postId);
    }

    /**
     * Получить посты с частыми изменениями
     */
    public List<Object[]> getFrequentlyChangedPosts(Long threshold) {
        return postAuditLogRepository.findFrequentlyChangedPosts(threshold);
    }

    /**
     * Получить активность по дням
     */
    public List<Object[]> getDailyActivity(LocalDateTime fromDate) {
        return postAuditLogRepository.findDailyActivity(fromDate);
    }

    /**
     * Маппинг PostAuditLog в PostAuditResponse
     */
    private PostAuditResponse mapToPostAuditResponse(PostAuditLog auditLog) {
        return PostAuditResponse.builder()
                .id(auditLog.getId())
                .postId(auditLog.getPostId())
                .operation(auditLog.getOperation())
                .changedAt(auditLog.getChangedAt())
                .userId(auditLog.getUserId())
                .oldTitle(auditLog.getOldTitle())
                .newTitle(auditLog.getNewTitle())
                .oldContent(auditLog.getOldContent())
                .newContent(auditLog.getNewContent())
                .oldIsPublished(auditLog.getOldIsPublished())
                .newIsPublished(auditLog.getNewIsPublished())
                .changeDetails(auditLog.getChangeDetails())
                .build();
    }
}