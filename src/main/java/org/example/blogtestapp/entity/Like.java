package org.example.blogtestapp.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность лайка поста
 * Связывает пользователя с постом, который он лайкнул
 */
@Entity
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Like extends BaseEntity {

    // Связь с пользователем
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Связь с постом
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}