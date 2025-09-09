package org.example.blogtestapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.dto.ActiveUserStatisticsResponse;
import org.example.blogtestapp.dto.CreatePostRequest;
import org.example.blogtestapp.dto.PostResponse;
import org.example.blogtestapp.dto.PostSummaryResponse;
import org.example.blogtestapp.dto.TopicStatisticsResponse;
import org.example.blogtestapp.dto.UpdatePostRequest;
import org.example.blogtestapp.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST Controller для работы с постами
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "API для управления постами блога")
public class PostController {

    private final PostService postService;

    /**
     * Создать новый пост
     */
    @Operation(summary = "Создать новый пост", description = "Создаёт новый пост в блоге")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пост успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostResponse.class))),
            @ApiResponse(responseCode = "400", description = "Невалидные данные", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Parameter(description = "Данные для создания поста", required = true)
            @Valid @RequestBody CreatePostRequest request,
            @Parameter(description = "Username автора поста", required = true, example = "john_doe")
            @RequestHeader("X-Author-Username") String authorUsername) {
        log.info("Creating post with title: {} by user: {}", request.getTitle(), authorUsername);
        try {
            PostResponse response = postService.createPost(request, authorUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Error while creating post", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить пост по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return postService.getPublishedPostById(id)
                .map(post -> ResponseEntity.ok(post))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить все опубликованные посты с пагинацией
     */
    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostSummaryResponse> posts = postService.getPublishedPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Получить посты пользователя
     */
    @GetMapping("/author/{username}")
    public ResponseEntity<List<PostSummaryResponse>> getPostsByUser(@PathVariable String username) {
        try {
            List<PostSummaryResponse> posts = postService.getPostsByUser(username);
            return ResponseEntity.ok(posts);
        } catch (IllegalArgumentException e) {
            log.error("Error while getting posts", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Обновить пост
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @RequestHeader("X-Author-Username") String authorUsername) {
        try {
            PostResponse response = postService.updatePost(id, request, authorUsername);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error while updating post", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Удалить пост
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @RequestHeader("X-Author-Username") String authorUsername) {
        try {
            postService.deletePost(id, authorUsername);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error while deleting post", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Полнотекстовый поиск постов
     */
    @GetMapping("/search/fulltext")
    public ResponseEntity<List<PostSummaryResponse>> fullTextSearch(@RequestParam String q) {
        List<PostSummaryResponse> posts = postService.fullTextSearch(URLDecoder.decode(q, StandardCharsets.UTF_8));
        return ResponseEntity.ok(posts);
    }

    /**
     * Простой поиск постов
     */
    @GetMapping("/search")
    public ResponseEntity<List<PostSummaryResponse>> searchPosts(@RequestParam String q) {
        List<PostSummaryResponse> posts = postService.searchPosts(URLDecoder.decode(q, StandardCharsets.UTF_8));
        return ResponseEntity.ok(posts);
    }

    /**
     * Получить посты по тегу
     */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<PostSummaryResponse>> getPostsByTag(@PathVariable String tagName) {
        List<PostSummaryResponse> posts = postService.getPostsByTag(tagName);
        return ResponseEntity.ok(posts);
    }

    /**
     * Получить популярные посты
     */
    @GetMapping("/popular")
    public ResponseEntity<List<PostSummaryResponse>> getPopularPosts(
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<PostSummaryResponse> posts = postService.getPopularPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Получить статистику постов по темам (группировка данных)
     */
    @GetMapping("/statistics/topics")
    public ResponseEntity<List<TopicStatisticsResponse>> getTopicStatistics() {
        List<TopicStatisticsResponse> statistics = postService.getTopicStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Получить статистику активных пользователей за последние 10 дней
     */
    @GetMapping("/statistics/active-users")
    public ResponseEntity<List<ActiveUserStatisticsResponse>> getActiveUsersStatistics() {
        List<ActiveUserStatisticsResponse> statistics = postService.getActiveUsersStatistics();
        return ResponseEntity.ok(statistics);
    }
}