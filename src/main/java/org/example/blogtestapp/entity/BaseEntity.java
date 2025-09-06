package org.example.blogtestapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Базовая сущность с полями аудита
 */
@MappedSuperclass
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Include
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ToString.Include
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}