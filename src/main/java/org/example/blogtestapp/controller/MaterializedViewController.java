package org.example.blogtestapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.service.MaterializedViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Административный контроллер для управления материализованными представлениями
 */
@RestController
@RequestMapping("/private/materialized-views")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Materialized Views", description = "Административные операции с материализованными представлениями")
public class MaterializedViewController {

    private final MaterializedViewService materializedViewService;

    /**
     * Принудительное обновление материализованного представления
     */
    @Operation(summary = "Принудительное обновление материализованного представления",
            description = "Запускает обновление материализованного представления с статистикой активных пользователей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Обновление выполнено успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Ошибка при обновлении", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> forceRefresh() {
        try {
            log.info("Force refresh request received");
            String result = materializedViewService.forceRefreshActiveUsersStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Force refresh failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Получение статистики обновлений материализованного представления
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = materializedViewService.getRefreshStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get statistics", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Инициализация daily database-level планировщика pg_cron (каждый день в 00:00)
     */
    @PostMapping("/initialize-scheduler")
    public ResponseEntity<Map<String, Object>> initializeScheduler() {
        try {
            log.info("Initialize daily scheduler request received");
            String result = materializedViewService.initializeDatabaseScheduler();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("scheduleInfo", "Daily refresh at 00:00 UTC");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to initialize daily scheduler", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Отключение daily database-level планировщика
     */
    @PostMapping("/disable-scheduler")
    public ResponseEntity<Map<String, Object>> disableScheduler() {
        try {
            log.info("Disable daily scheduler request received");
            String result = materializedViewService.disableDatabaseScheduler();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("fallbackInfo", "Application will use daily fallback scheduling");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to disable daily scheduler", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Получение статуса daily pg_cron задачи
     */
    @GetMapping("/cron-status")
    public ResponseEntity<Map<String, Object>> getCronStatus() {
        try {
            Map<String, Object> cronStatus = materializedViewService.getCronJobStatus();
            boolean pgCronAvailable = materializedViewService.isPgCronAvailable();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pgCronAvailable", pgCronAvailable);
            response.put("dailyJobInfo", cronStatus);
            response.put("scheduleDescription", "Daily at 00:00 UTC");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get daily cron status", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Комплексная информация о состоянии системы материализованных представлений
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Проверка доступности pg_cron
            boolean pgCronAvailable = materializedViewService.isPgCronAvailable();
            health.put("pgCronAvailable", pgCronAvailable);
            
            // Статус daily cron job
            Map<String, Object> cronStatus = materializedViewService.getCronJobStatus();
            boolean dailyJobActive = Boolean.TRUE.equals(cronStatus.get("jobExists")) && 
                                   Boolean.TRUE.equals(cronStatus.get("active"));
            health.put("dailyJobActive", dailyJobActive);
            health.put("dailyJobDetails", cronStatus);
            
            // Статистика обновлений
            Map<String, Object> refreshStats = materializedViewService.getRefreshStatistics();
            health.put("refreshStatistics", refreshStats);
            
            // Определение общего статуса
            String overallStatus;
            String statusDescription;
            if (pgCronAvailable && dailyJobActive) {
                overallStatus = "OPTIMAL";
                statusDescription = "Daily database-level refresh at 00:00 UTC is active";
            } else if (pgCronAvailable) {
                overallStatus = "AVAILABLE";
                statusDescription = "pg_cron available but daily job not configured. Use /initialize-scheduler to set up.";
            } else {
                overallStatus = "FALLBACK";
                statusDescription = "Using application-level daily fallback scheduling";
            }
            health.put("overallStatus", overallStatus);
            health.put("statusDescription", statusDescription);
            health.put("refreshSchedule", "Daily at 00:00 UTC");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("health", health);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get health status", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}