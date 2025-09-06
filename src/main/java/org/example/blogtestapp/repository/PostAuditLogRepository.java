package org.example.blogtestapp.repository;

import org.example.blogtestapp.entity.PostAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository для работы с журналом аудита постов
 */
@Repository
public interface PostAuditLogRepository extends JpaRepository<PostAuditLog, Long> {

    /**
     * Найти все записи аудита для конкретного поста
     */
    List<PostAuditLog> findByPostIdOrderByChangedAtDesc(Long postId);

    /**
     * Найти записи аудита по типу операции
     */
    List<PostAuditLog> findByOperationOrderByChangedAtDesc(String operation);

    /**
     * Найти записи аудита за определенный период
     */
    List<PostAuditLog> findByChangedAtBetweenOrderByChangedAtDesc(
        LocalDateTime startDate, 
        LocalDateTime endDate
    );

    /**
     * Найти записи аудита для конкретного пользователя
     */
    List<PostAuditLog> findByUserIdOrderByChangedAtDesc(Long userId);

    /**
     * Найти записи аудита поста для пользователя
     */
    List<PostAuditLog> findByPostIdAndUserIdOrderByChangedAtDesc(Long postId, Long userId);

    /**
     * Найти последние изменения (общий аудит)
     */
    @Query("SELECT pal FROM PostAuditLog pal ORDER BY pal.changedAt DESC")
    List<PostAuditLog> findRecentChanges();

    /**
     * Найти изменения заголовков (когда изменялся title)
     */
    @Query("SELECT pal FROM PostAuditLog pal WHERE pal.operation = 'UPDATE' " +
           "AND pal.oldTitle != pal.newTitle ORDER BY pal.changedAt DESC")
    List<PostAuditLog> findTitleChanges();

    /**
     * Найти удаленные посты
     */
    @Query("SELECT pal FROM PostAuditLog pal WHERE pal.operation = 'DELETE' " +
           "ORDER BY pal.changedAt DESC")
    List<PostAuditLog> findDeletedPosts();

    /**
     * Подсчитать количество изменений поста
     */
    @Query("SELECT COUNT(pal) FROM PostAuditLog pal WHERE pal.postId = :postId")
    Long countChangesByPostId(@Param("postId") Long postId);

    /**
     * Найти частые изменения (посты с большим количеством изменений)
     */
    @Query("SELECT pal.postId, COUNT(pal) as changeCount FROM PostAuditLog pal " +
           "GROUP BY pal.postId HAVING COUNT(pal) > :threshold " +
           "ORDER BY COUNT(pal) DESC")
    List<Object[]> findFrequentlyChangedPosts(@Param("threshold") Long threshold);

    /**
     * Найти активность по дням
     */
    @Query("SELECT DATE(pal.changedAt), COUNT(pal) FROM PostAuditLog pal " +
           "WHERE pal.changedAt >= :fromDate " +
           "GROUP BY DATE(pal.changedAt) " +
           "ORDER BY DATE(pal.changedAt) DESC")
    List<Object[]> findDailyActivity(@Param("fromDate") LocalDateTime fromDate);
}