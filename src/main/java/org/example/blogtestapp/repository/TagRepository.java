package org.example.blogtestapp.repository;

import org.example.blogtestapp.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с тегами
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Найти тег по имени
     */
    Optional<Tag> findByName(String name);

    /**
     * Проверить существует ли тег с таким именем
     */
    boolean existsByName(String name);

    /**
     * Найти активные теги
     */
    List<Tag> findByIsActiveTrueOrderByUsageCountDesc();

    /**
     * Найти теги по части имени
     */
    @Query("SELECT t FROM Tag t WHERE t.isActive = true AND " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.usageCount DESC")
    List<Tag> findByNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * Найти самые популярные теги
     */
    @Query("SELECT t FROM Tag t WHERE t.isActive = true " +
           "ORDER BY t.usageCount DESC")
    List<Tag> findMostPopularTags();

    /**
     * Найти теги, которые используются в постах
     */
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.posts p " +
           "WHERE t.isActive = true AND p.isPublished = true " +
           "ORDER BY t.usageCount DESC")
    List<Tag> findTagsUsedInPublishedPosts();

    /**
     * Увеличить счетчик использования тега
     */
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + 1 WHERE t.id = :tagId")
    void incrementUsageCount(@Param("tagId") Long tagId);

    /**
     * Уменьшить счетчик использования тега
     */
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount - 1 WHERE t.id = :tagId AND t.usageCount > 0")
    void decrementUsageCount(@Param("tagId") Long tagId);

    /**
     * Обновить счетчики использования всех тегов
     */
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = (" +
           "SELECT COUNT(pt) FROM Post p JOIN p.tags pt WHERE pt.id = t.id AND p.isPublished = true)")
    void updateAllUsageCounts();
}