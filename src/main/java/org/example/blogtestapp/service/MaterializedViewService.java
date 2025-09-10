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

    /**
     * Fallback обновление materialized view (только если database job не активен)
     * Теперь используется только как резерв, основное обновление делает pg_cron daily job
     */
    @Scheduled(cron = "0 * * * * *") // 24 hours - daily
    @Transactional
    public void refreshActiveUsersStatistics() {
        try {
            // Проверяем, активен ли database job
            if (isPgCronAvailable() && isCronJobExists()) {
                log.debug("Daily database job is active, skipping application-level refresh");
                return;
            }

            log.info("Daily database job not active, using application-level refresh");

            // Try to use the database function first, fallback to direct refresh
            refreshActiveUsersMv();
            log.info("Successfully refreshed using database function");
        } catch (Exception e) {
            log.error("Failed to refresh mv using application job", e);
        }
    }

    private void refreshActiveUsersMv() {
        entityManager.createNativeQuery("SELECT refresh_active_users_mv()").getSingleResult();
    }

    /**
     * Принудительное обновление materialized view
     */
    @Transactional
    public String forceRefreshActiveUsersStatistics() {
        log.info("Force refreshing active users statistics materialized view");
        long startTime = System.currentTimeMillis();
        try {
            refreshActiveUsersMv();
            log.info("Force refresh completed using database function");
        } catch (Exception e) {
            log.error("Failed to force refresh active users statistics materialized view: {}", e.getMessage(), e);
            return String.format("Unable to force refresh active users statistics materialized view: %s", e.getMessage());
        }
        long duration = System.currentTimeMillis() - startTime;
        String result = String.format("Successfully refreshed materialized view in %d ms", duration);
        log.info(result);
        return result;
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

    /**
     * Проверка наличии джобы на уровне БД
     */
    private boolean isCronJobExists() {
        try {
            String checkSql = "SELECT EXISTS(SELECT 1 FROM cron.job WHERE jobname = 'daily-active-users-mv-refresh-job')";
            Boolean result = (Boolean) entityManager.createNativeQuery(checkSql).getSingleResult();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.debug("Cannot check pg_cron daily-active-users-mv-refresh-job availability: {}", e.getMessage());
            return false;
        }
    }
}