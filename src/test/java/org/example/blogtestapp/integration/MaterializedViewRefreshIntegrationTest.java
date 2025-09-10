package org.example.blogtestapp.integration;

import org.example.blogtestapp.service.MaterializedViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for materialized view refresh functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MaterializedViewRefreshIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testForceRefreshMaterializedView() {
        // When: Force refresh materialized view
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/private/materialized-views/refresh", 
            null, 
            Map.class
        );

        // Then: Should refresh successfully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("message")).asString().contains("Successfully refreshed");
    }

    @Test
    public void testGetRefreshStatistics() {
        // When: Get refresh statistics
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/private/materialized-views/statistics", 
            Map.class
        );

        // Then: Should return statistics successfully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("statistics")).isNotNull();
    }
}