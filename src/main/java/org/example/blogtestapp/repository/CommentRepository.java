package org.example.blogtestapp.repository;

import org.example.blogtestapp.entity.Comment;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository для работы с комментариями
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Найти комментарии к посту (только одобренные и не удаленные)
     */
    List<Comment> findByPostAndIsApprovedTrueAndIsDeletedFalseOrderByCreatedAtAsc(Post post);

    /**
     * Найти все комментарии к посту (включая неодобренные, для модерации)
     */
    List<Comment> findByPostOrderByCreatedAtDesc(Post post);

    /**
     * Найти комментарии пользователя
     */
    List<Comment> findByAuthorAndIsDeletedFalseOrderByCreatedAtDesc(User author);

    /**
     * Найти дочерние комментарии (ответы)
     */
    List<Comment> findByParentCommentAndIsApprovedTrueAndIsDeletedFalseOrderByCreatedAtAsc(Comment parentComment);

    /**
     * Найти корневые комментарии к посту (без родительского комментария)
     */
    List<Comment> findByPostAndParentCommentIsNullAndIsApprovedTrueAndIsDeletedFalseOrderByCreatedAtAsc(Post post);

    /**
     * Подсчитать количество комментариев к посту
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post = :post " +
           "AND c.isApproved = true AND c.isDeleted = false")
    Long countByPost(@Param("post") Post post);

    /**
     * Найти неодобренные комментарии (для модерации)
     */
    List<Comment> findByIsApprovedFalseAndIsDeletedFalseOrderByCreatedAtDesc();

    /**
     * Найти последние комментарии
     */
    @Query("SELECT c FROM Comment c WHERE c.isApproved = true AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments();

    /**
     * Поиск комментариев по содержимому
     */
    @Query("SELECT c FROM Comment c WHERE c.isApproved = true AND c.isDeleted = false " +
           "AND LOWER(c.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findByContentContaining(@Param("searchTerm") String searchTerm);
}