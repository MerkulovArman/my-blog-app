package org.example.blogtestapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

/**
 * Сущность тега
 * Реализация через отдельную таблицу для лучшей нормализации и гибкости поиска
 * Обоснование выбора: отдельная таблица позволяет:
 * 1. Нормализацию данных (избежание дублирования названий тегов)
 * 2. Эффективные индексы для поиска по тегам
 * 3. Возможность добавления метаданных к тегам (описание, цвет, статистика использования)
 * 4. Контроль уникальности названий тегов
 * 5. Эффективные join'ы для фильтрации постов по тегам
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag extends BaseEntity {

    @NotBlank(message = "Tag name is required")
    @Size(min = 1, max = 50, message = "Tag name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я0-9_-]+$", message = "Tag name can only contain letters, numbers, underscores and hyphens")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 200, message = "Tag description cannot exceed 200 characters")
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Связь с постами
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Post> posts;
}