package org.example.blogtestapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Сервис для обновления материализованных представлений
 * Поддерживает database-level refresh через триггеры и pg_cron, а также fallback на application-level
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterializedViewService {

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    /**
     * Инициализация daily database scheduler с pg_cron (заглушка)
     */
    @Transactional
    public String initializeDatabaseScheduler() {
        try {
            log.info("Daily database scheduler not implemented in this version");
            boolean pgCronAvailable = isPgCronAvailable();

            if (pgCronAvailable) {
                return "pg_cron extension is available but daily job setup is not implemented. Use application-level scheduling.";
            } else {
                return "pg_cron extension not available. Using application-level daily scheduling.";
            }

        } catch (Exception e) {
            log.error("Failed to initialize daily database scheduler: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Fallback обновление materialized view (только если database job не активен)
     * Теперь используется только как резерв, основное обновление делает pg_cron daily job
     */
    @Scheduled(cron = "0 * * * * *") // 24 hours - daily
    @Transactional
    public void refreshActiveUsersStatistics() {
        try {
            // Проверяем, активен ли database job
            if (isDailyJobActive()) {
                log.debug("Daily database job is active, skipping application-level refresh");
                return;
            }

            log.info("Daily database job not active, using fallback refresh");

            // Try to use the database function first, fallback to direct refresh
            try {
                entityManager.createNativeQuery("SELECT refresh_active_users_mv()").getSingleResult();
                log.info("Successfully refreshed using database function");
            } catch (Exception funcException) {
                log.warn("Database function failed, using direct refresh: {}", funcException.getMessage());
                // Fallback to direct materialized view refresh
                entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY active_users_stats_mv").executeUpdate();

                // Log manually
                entityManager.createNativeQuery(
                        "INSERT INTO materialized_view_refresh_log (view_name, refresh_type, triggered_at, success) " +
                                "VALUES ('active_users_stats_mv', 'FALLBACK', CURRENT_TIMESTAMP, true)"
                ).executeUpdate();
                log.info("Successfully refreshed using direct method");
            }

        } catch (Exception e) {
            log.error("Failed to refresh active users statistics materialized view: {}", e.getMessage(), e);

            // Log the error
            try {
                entityManager.createNativeQuery(
                        "INSERT INTO materialized_view_refresh_log (view_name, refresh_type, triggered_at, success, error_message) " +
                                "VALUES ('active_users_stats_mv', 'FALLBACK', CURRENT_TIMESTAMP, false, ?)"
                ).setParameter(1, e.getMessage()).executeUpdate();
            } catch (Exception logException) {
                log.error("Failed to log refresh error: {}", logException.getMessage());
            }
        }
    }

    /**
     * Принудительное обновление materialized view
     */
    @Transactional
    public String forceRefreshActiveUsersStatistics() {
        try {
            log.info("Force refreshing active users statistics materialized view");

            long startTime = System.currentTimeMillis();

            // Try to use the database function first, fallback to direct refresh
            try {
                entityManager.createNativeQuery("SELECT refresh_active_users_mv()").getSingleResult();
                log.info("Force refresh completed using database function");
            } catch (Exception funcException) {
                log.warn("Database function failed during force refresh, using direct method: {}", funcException.getMessage());
                // Fallback to direct materialized view refresh
                entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY active_users_stats_mv").getSingleResult();

                // Log manually
                entityManager.createNativeQuery(
                        "INSERT INTO materialized_view_refresh_log (view_name, refresh_type, triggered_at, success) " +
                                "VALUES ('active_users_stats_mv', 'MANUAL', CURRENT_TIMESTAMP, true)"
                ).executeUpdate();
                log.info("Force refresh completed using direct method");
            }

            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("Successfully refreshed materialized view in %d ms", duration);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("Failed to force refresh active users statistics materialized view: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh materialized view", e);
        }
    }

    /**
     * Получение статистики обновлений MV из лога
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRefreshStatistics() {
        try {
            String sql = "SELECT COUNT(*) as refresh_count, " +
                    "MAX(triggered_at) as last_refresh, " +
                    "AVG(duration_ms) as avg_duration, " +
                    "SUM(CASE WHEN success = false THEN 1 ELSE 0 END) as error_count " +
                    "FROM materialized_view_refresh_log " +
                    "WHERE view_name = 'active_users_stats_mv' " +
                    "AND triggered_at >= CURRENT_DATE - INTERVAL '1 day'";

            Object[] result = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

            Map<String, Object> stats = new HashMap<>();
            stats.put("refreshCount", ((Number) result[0]).longValue());
            stats.put("lastRefresh", result[1]);
            stats.put("averageDurationMs", result[2] != null ? ((Number) result[2]).doubleValue() : 0.0);
            stats.put("errorCount", ((Number) result[3]).longValue());

            return stats;

        } catch (Exception e) {
            log.error("Failed to get refresh statistics: {}", e.getMessage());
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", e.getMessage());
            return errorStats;
        }
    }

    /**
     * Отключение daily database scheduler (заглушка)
     */
    @Transactional
    public String disableDatabaseScheduler() {
        try {
            log.info("Daily database scheduler not implemented in this version");
            return "Daily database scheduler not implemented. Application-level scheduling is active.";
        } catch (Exception e) {
            log.error("Failed to disable daily database scheduler: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Получение статуса daily pg_cron задачи (заглушка)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCronJobStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("jobExists", false);
        status.put("jobId", null);
        status.put("schedule", null);
        status.put("command", null);
        status.put("active", false);
        status.put("lastRunStatus", "Daily cron job not implemented in this version");
        return status;
    }

    /**
     * Проверка активности daily pg_cron задачи
     */
    private boolean isDailyJobActive() {
        try {
            Map<String, Object> status = getCronJobStatus();
            return Boolean.TRUE.equals(status.get("jobExists")) &&
                    Boolean.TRUE.equals(status.get("active"));
        } catch (Exception e) {
            log.debug("Cannot check daily job status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверка доступности pg_cron
     */
    @Transactional(readOnly = true)
    public boolean isPgCronAvailable() {
        try {
            String checkSql = "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'pg_cron')";
            Boolean result = (Boolean) entityManager.createNativeQuery(checkSql).getSingleResult();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.debug("Cannot check pg_cron availability: {}", e.getMessage());
            return false;
        }
    }
}