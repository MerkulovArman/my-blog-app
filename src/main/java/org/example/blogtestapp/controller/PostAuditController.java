package org.example.blogtestapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.dto.PostAuditResponse;
import org.example.blogtestapp.service.PostAuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller для работы с аудитом постов
 * Доступен по пути /private/audit-info как служебный endpoint
 */
@RestController
@RequestMapping("/private/audit-info")
@RequiredArgsConstructor
@Slf4j
public class PostAuditController {

    private final PostAuditService postAuditService;

    /**
     * Получить историю изменений поста
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<PostAuditResponse>> getPostAuditHistory(@PathVariable Long postId) {
        log.info("Getting audit history for post ID: {}", postId);
        List<PostAuditResponse> auditHistory = postAuditService.getPostAuditHistory(postId);
        return ResponseEntity.ok(auditHistory);
    }

    /**
     * Получить все записи аудита по типу операции
     */
    @GetMapping("/operation/{operation}")
    public ResponseEntity<List<PostAuditResponse>> getAuditByOperation(@PathVariable String operation) {
        List<PostAuditResponse> auditRecords = postAuditService.getAuditByOperation(operation.toUpperCase());
        return ResponseEntity.ok(auditRecords);
    }

    /**
     * Получить записи аудита за период
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<PostAuditResponse>> getAuditByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<PostAuditResponse> auditRecords = postAuditService.getAuditByDateRange(startDate, endDate);
        return ResponseEntity.ok(auditRecords);
    }

    /**
     * Получить последние изменения
     */
    @GetMapping("/recent")
    public ResponseEntity<List<PostAuditResponse>> getRecentChanges(
            @RequestParam(defaultValue = "50") int limit) {
        List<PostAuditResponse> recentChanges = postAuditService.getRecentChanges(limit);
        return ResponseEntity.ok(recentChanges);
    }

    /**
     * Получить изменения заголовков
     */
    @GetMapping("/title-changes")
    public ResponseEntity<List<PostAuditResponse>> getTitleChanges() {
        List<PostAuditResponse> titleChanges = postAuditService.getTitleChanges();
        return ResponseEntity.ok(titleChanges);
    }

    /**
     * Получить удаленные посты
     */
    @GetMapping("/deleted-posts")
    public ResponseEntity<List<PostAuditResponse>> getDeletedPosts() {
        List<PostAuditResponse> deletedPosts = postAuditService.getDeletedPosts();
        return ResponseEntity.ok(deletedPosts);
    }

    /**
     * Получить статистику изменений поста
     */
    @GetMapping("/post/{postId}/stats")
    public ResponseEntity<Long> getPostChangeCount(@PathVariable Long postId) {
        Long changeCount = postAuditService.getPostChangeCount(postId);
        return ResponseEntity.ok(changeCount);
    }

    /**
     * Получить посты с частыми изменениями
     */
    @GetMapping("/frequently-changed")
    public ResponseEntity<List<Object[]>> getFrequentlyChangedPosts(
            @RequestParam(defaultValue = "5") Long threshold) {
        List<Object[]> frequentlyChangedPosts = postAuditService.getFrequentlyChangedPosts(threshold);
        return ResponseEntity.ok(frequentlyChangedPosts);
    }

    /**
     * Получить активность по дням
     */
    @GetMapping("/daily-activity")
    public ResponseEntity<List<Object[]>> getDailyActivity(
            @RequestParam(defaultValue = "30") int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<Object[]> dailyActivity = postAuditService.getDailyActivity(fromDate);
        return ResponseEntity.ok(dailyActivity);
    }
}