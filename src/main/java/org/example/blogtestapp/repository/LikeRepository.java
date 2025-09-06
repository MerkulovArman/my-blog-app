package org.example.blogtestapp.repository;

import org.example.blogtestapp.entity.Like;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с лайками
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * Найти лайк пользователя для конкретного поста
     */
    Optional<Like> findByUserAndPost(User user, Post post);

    /**
     * Проверить лайкнул ли пользователь пост
     */
    boolean existsByUserAndPostAndIsActiveTrue(User user, Post post);

    /**
     * Найти все лайки поста
     */
    List<Like> findByPostAndIsActiveTrueOrderByCreatedAtDesc(Post post);

    /**
     * Найти все лайки пользователя
     */
    List<Like> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);

    /**
     * Подсчитать количество лайков поста
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.post = :post AND l.isActive = true")
    Long countByPost(@Param("post") Post post);

    /**
     * Подсчитать количество лайков пользователя
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.user = :user AND l.isActive = true")
    Long countByUser(@Param("user") User user);

    /**
     * Найти самые лайкаемые посты
     */
    @Query("SELECT l.post FROM Like l WHERE l.isActive = true " +
           "GROUP BY l.post " +
           "ORDER BY COUNT(l) DESC")
    List<Post> findMostLikedPosts();

    /**
     * Найти пользователей, которые лайкнули пост
     */
    @Query("SELECT l.user FROM Like l WHERE l.post = :post AND l.isActive = true " +
           "ORDER BY l.createdAt DESC")
    List<User> findUsersWhoLikedPost(@Param("post") Post post);

    /**
     * Удалить все лайки пользователя (при удалении пользователя)
     */
    void deleteByUser(User user);

    /**
     * Удалить все лайки поста (при удалении поста)
     */
    void deleteByPost(Post post);
}