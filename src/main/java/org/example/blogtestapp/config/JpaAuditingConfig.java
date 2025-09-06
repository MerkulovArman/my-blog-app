package org.example.blogtestapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Конфигурация JPA аудита для автоматического заполнения полей createdAt и updatedAt
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}