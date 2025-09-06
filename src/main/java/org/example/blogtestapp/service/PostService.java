package org.example.blogtestapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.dto.*;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.Tag;
import org.example.blogtestapp.entity.User;
import org.example.blogtestapp.repository.PostRepository;
import org.example.blogtestapp.repository.TagRepository;
import org.example.blogtestapp.repository.UserRepository;
import org.example.blogtestapp.repository.LikeRepository;
import org.example.blogtestapp.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для работы с постами
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    /**
     * Создать новый пост
     */
    public PostResponse createPost(CreatePostRequest request, String authorUsername) {
        log.info("Creating new post with title: {} by user: {}", request.getTitle(), authorUsername);

        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + authorUsername));

        // Обработка тегов
        Set<Tag> tags = processTagNames(request.getTagNames());

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isPublished(request.getIsPublished())
                .publishedAt(request.getIsPublished() ? LocalDateTime.now() : null)
                .author(author)
                .tags(tags)
                .viewsCount(0L)
                .build();

        Post savedPost = postRepository.save(post);

        // Обновляем счетчики использования тегов
        updateTagUsageCounts(tags, 1);

        log.info("Post created successfully with ID: {}", savedPost.getId());
        return mapToPostResponse(savedPost);
    }

    /**
     * Получить пост по ID
     */
    @Transactional(readOnly = true)
    public Optional<PostResponse> getPostById(Long id) {
        return postRepository.findById(id)
                .map(this::mapToPostResponse);
    }

    /**
     * Получить опубликованный пост по ID (с увеличением счетчика просмотров)
     */
    public Optional<PostResponse> getPublishedPostById(Long id) {
        Optional<Post> postOpt = postRepository.findByIdAndIsPublishedTrue(id);
        if (postOpt.isPresent()) {
            // Увеличиваем счетчик просмотров
            postRepository.incrementViewCount(id);
            return postOpt.map(this::mapToPostResponse);
        }
        return Optional.empty();
    }

    /**
     * Получить все опубликованные посты с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPublishedPosts(Pageable pageable) {
        return postRepository.findByIsPublishedTrueOrderByPublishedAtDesc(pageable)
                .map(this::mapToPostSummaryResponse);
    }

    /**
     * Получить посты пользователя
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getPostsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return postRepository.findByAuthorOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToPostSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Обновить пост
     */
    public PostResponse updatePost(Long id, UpdatePostRequest request, String authorUsername) {
        log.info("Updating post with ID: {} by user: {}", id, authorUsername);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + id));

        // Проверяем права доступа
        if (!post.getAuthor().getUsername().equals(authorUsername)) {
            throw new IllegalArgumentException("User does not have permission to update this post");
        }

        // Сохраняем старые теги для обновления счетчиков
        Set<Tag> oldTags = new HashSet<>(post.getTags());

        // Обновляем поля
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getIsPublished() != null) {
            boolean wasPublished = post.getIsPublished();
            post.setIsPublished(request.getIsPublished());
            
            // Устанавливаем дату публикации при первой публикации
            if (request.getIsPublished() && !wasPublished) {
                post.setPublishedAt(LocalDateTime.now());
            } else if (!request.getIsPublished()) {
                post.setPublishedAt(null);
            }
        }
        if (request.getTagNames() != null) {
            Set<Tag> newTags = processTagNames(request.getTagNames());
            post.setTags(newTags);
            
            // Обновляем счетчики тегов
            updateTagUsageCounts(oldTags, -1);
            updateTagUsageCounts(newTags, 1);
        }

        Post savedPost = postRepository.save(post);
        log.info("Post updated successfully with ID: {}", savedPost.getId());

        return mapToPostResponse(savedPost);
    }

    /**
     * Удалить пост
     */
    public void deletePost(Long id, String authorUsername) {
        log.info("Deleting post with ID: {} by user: {}", id, authorUsername);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + id));

        // Проверяем права доступа
        if (!post.getAuthor().getUsername().equals(authorUsername)) {
            throw new IllegalArgumentException("User does not have permission to delete this post");
        }

        // Уменьшаем счетчики использования тегов
        updateTagUsageCounts(post.getTags(), -1);

        postRepository.delete(post);
        log.info("Post deleted successfully with ID: {}", id);
    }

    /**
     * Полнотекстовый поиск постов
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> fullTextSearch(String searchQuery) {
        return postRepository.fullTextSearch(searchQuery)
                .stream()
                .map(this::mapToPostSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Поиск постов по заголовку и содержимому
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> searchPosts(String searchTerm) {
        return postRepository.findByTitleOrContentContaining(searchTerm)
                .stream()
                .map(this::mapToPostSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить посты по тегу
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getPostsByTag(String tagName) {
        return postRepository.findByTagName(tagName)
                .stream()
                .map(this::mapToPostSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить популярные посты
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getPopularPosts(Pageable pageable) {
        return postRepository.findPopularPosts(pageable)
                .stream()
                .map(this::mapToPostSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Обработка названий тегов
     */
    private Set<Tag> processTagNames(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                                .name(tagName)
                                .usageCount(0L)
                                .isActive(true)
                                .build();
                        return tagRepository.save(newTag);
                    });
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Обновление счетчиков использования тегов
     */
    private void updateTagUsageCounts(Set<Tag> tags, int delta) {
        for (Tag tag : tags) {
            if (delta > 0) {
                tagRepository.incrementUsageCount(tag.getId());
            } else if (delta < 0) {
                tagRepository.decrementUsageCount(tag.getId());
            }
        }
    }

    /**
     * Маппинг Post в PostResponse
     */
    private PostResponse mapToPostResponse(Post post) {
        Set<String> tagNames = post.getTags() != null ?
                post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet()) : new HashSet<>();

        Long likesCount = likeRepository.countByPost(post);
        Long commentsCount = commentRepository.countByPost(post);

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .publishedAt(post.getPublishedAt())
                .isPublished(post.getIsPublished())
                .viewsCount(post.getViewsCount())
                .authorUsername(post.getAuthor().getUsername())
                .authorId(post.getAuthor().getId())
                .tagNames(tagNames)
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * Маппинг Post в PostSummaryResponse
     */
    private PostSummaryResponse mapToPostSummaryResponse(Post post) {
        Set<String> tagNames = post.getTags() != null ?
                post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet()) : new HashSet<>();

        String excerpt = post.getContent().length() > 200 ?
                post.getContent().substring(0, 200) + "..." : post.getContent();

        Long likesCount = likeRepository.countByPost(post);
        Long commentsCount = commentRepository.countByPost(post);

        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .excerpt(excerpt)
                .publishedAt(post.getPublishedAt())
                .authorUsername(post.getAuthor().getUsername())
                .tagNames(tagNames)
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .viewsCount(post.getViewsCount())
                .build();
    }
}