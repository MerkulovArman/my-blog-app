package org.example.blogtestapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность для журнала аудита изменений постов
 * Записывается через триггер при изменении/удалении постов
 */
@Entity
@Table(name = "post_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "operation", nullable = false, length = 10)
    private String operation; // INSERT, UPDATE, DELETE

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "old_title", length = 200)
    private String oldTitle;

    @Column(name = "new_title", length = 200)
    private String newTitle;

    @Column(name = "old_content", columnDefinition = "TEXT")
    private String oldContent;

    @Column(name = "new_content", columnDefinition = "TEXT")
    private String newContent;

    @Column(name = "old_is_published")
    private Boolean oldIsPublished;

    @Column(name = "new_is_published")
    private Boolean newIsPublished;

    @Column(name = "change_details", columnDefinition = "TEXT")
    private String changeDetails;
}