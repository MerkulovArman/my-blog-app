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
}