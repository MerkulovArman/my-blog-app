package org.example.blogtestapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.dto.*;
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
public class PostController {

    private final PostService postService;

    /**
     * Создать новый пост
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @RequestHeader("X-Author-Username") String authorUsername) {
        log.info("Creating post with title: {} by user: {}", request.getTitle(), authorUsername);
        try {
            PostResponse response = postService.createPost(request, authorUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
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
}