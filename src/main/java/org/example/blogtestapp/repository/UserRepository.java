package org.example.blogtestapp.repository;

import org.example.blogtestapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по username
     */
    Optional<User> findByUsername(String username);

    /**
     * Найти пользователя по email
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверить существует ли пользователь с таким username
     */
    boolean existsByUsername(String username);

    /**
     * Проверить существует ли пользователь с таким email
     */
    boolean existsByEmail(String email);

    /**
     * Найти активных пользователей
     */
    List<User> findByIsActiveTrue();

    /**
     * Найти пользователей по части имени (поиск)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findBySearchTerm(@Param("searchTerm") String searchTerm);

    /**
     * Найти топ пользователей по количеству постов
     */
    @Query("SELECT u FROM User u LEFT JOIN u.posts p " +
           "WHERE u.isActive = true " +
           "GROUP BY u.id " +
           "ORDER BY COUNT(p) DESC")
    List<User> findTopUsersByPostCount();
}