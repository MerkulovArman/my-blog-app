package org.example.blogtestapp.repository;

import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с постами
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Найти опубликованные посты с пагинацией
     */
    Page<Post> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    /**
     * Найти посты пользователя
     */
    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    /**
     * Найти опубликованные посты пользователя
     */
    List<Post> findByAuthorAndIsPublishedTrueOrderByPublishedAtDesc(User author);

    /**
     * Найти посты по тегу
     */
    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t " +
           "WHERE t.name = :tagName AND p.isPublished = true " +
           "ORDER BY p.publishedAt DESC")
    List<Post> findByTagName(@Param("tagName") String tagName);

    /**
     * Полнотекстовый поиск постов с использованием PostgreSQL tsvector
     */
    @Query(value = "SELECT * FROM posts p WHERE p.is_published = true " +
            "AND p.search_vector @@ to_tsquery('russian', :searchQuery) " +
            "ORDER BY ts_rank(p.search_vector, to_tsquery('russian', :searchQuery)) DESC",
//                   "AND p.search_vector @@ plainto_tsquery('russian', :searchQuery) " +
//                   "ORDER BY ts_rank(p.search_vector, plainto_tsquery('russian', :searchQuery)) DESC",
           nativeQuery = true)
    List<Post> fullTextSearch(@Param("searchQuery") String searchQuery);

    /**
     * Поиск постов по заголовку и содержимому (простой LIKE поиск)
     */
    @Query("SELECT p FROM Post p WHERE p.isPublished = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.publishedAt DESC")
    List<Post> findByTitleOrContentContaining(@Param("searchTerm") String searchTerm);

    /**
     * Найти популярные посты (по количеству лайков)
     */
    @Query("SELECT p FROM Post p LEFT JOIN p.likes l " +
           "WHERE p.isPublished = true " +
           "GROUP BY p.id " +
           "ORDER BY COUNT(l) DESC")
    List<Post> findPopularPosts(Pageable pageable);

    /**
     * Найти недавние посты за определенный период
     */
    @Query("SELECT p FROM Post p WHERE p.isPublished = true " +
           "AND p.publishedAt >= :fromDate " +
           "ORDER BY p.publishedAt DESC")
    List<Post> findRecentPosts(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Подсчитать количество опубликованных постов пользователя
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.author = :author AND p.isPublished = true")
    Long countPublishedPostsByAuthor(@Param("author") User author);

    /**
     * Увеличить счетчик просмотров
     */
    @Modifying
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    /**
     * Найти пост по ID только если он опубликован
     */
    Optional<Post> findByIdAndIsPublishedTrue(Long id);

    /**
     * Найти похожие посты по тегам
     */
    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t " +
           "WHERE t IN (SELECT pt.tags FROM Post pt WHERE pt.id = :postId) " +
           "AND p.id != :postId AND p.isPublished = true " +
           "ORDER BY p.publishedAt DESC")
    List<Post> findSimilarPosts(@Param("postId") Long postId, Pageable pageable);
}