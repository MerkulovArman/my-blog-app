package org.example.blogtestapp.integration;

import org.example.blogtestapp.TestcontainersConfiguration;
import org.example.blogtestapp.dto.CreatePostRequest;
import org.example.blogtestapp.dto.PostResponse;
import org.example.blogtestapp.dto.PostSummaryResponse;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.User;
import org.example.blogtestapp.repository.PostRepository;
import org.example.blogtestapp.repository.UserRepository;
import org.example.blogtestapp.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для PostController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PostControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    private String testUsername = "testauthor";
    private User testUser;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username(testUsername)
                .email("author@example.com")
                .displayName("Test Author")
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldCreatePost() {
        // Given
        CreatePostRequest request = CreatePostRequest.builder()
                .title("Test Post Title")
                .content("This is test post content")
                .isPublished(false)
                .tagNames(Set.of("java", "spring", "testing"))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Author-Username", testUsername);
        HttpEntity<CreatePostRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<PostResponse> response = restTemplate.exchange(
                "/posts", HttpMethod.POST, entity, PostResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Test Post Title");
        assertThat(response.getBody().getContent()).isEqualTo("This is test post content");
        assertThat(response.getBody().getIsPublished()).isFalse();
        assertThat(response.getBody().getAuthorUsername()).isEqualTo(testUsername);
        assertThat(response.getBody().getTagNames()).hasSize(3);

        // Verify post was saved to database
        assertThat(postRepository.count()).isEqualTo(1);
        List<PostSummaryResponse> savedPosts = postService.getPostsByUser(testUsername);
        assertThat(savedPosts.size()).isEqualTo(1);
        assertThat(savedPosts.get(0).getTitle()).isEqualTo("Test Post Title");
        assertThat(savedPosts.get(0).getTagNames()).hasSize(3);
    }

    @Test
    void shouldCreatePublishedPost() {
        // Given
        CreatePostRequest request = CreatePostRequest.builder()
                .title("Published Post")
                .content("This post should be published immediately")
                .isPublished(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Author-Username", testUsername);
        HttpEntity<CreatePostRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<PostResponse> response = restTemplate.exchange(
                "/posts", HttpMethod.POST, entity, PostResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsPublished()).isTrue();
        assertThat(response.getBody().getPublishedAt()).isNotNull();

        // Verify published date was set
        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost.getPublishedAt()).isNotNull();
        assertThat(savedPost.getPublishedAt()).isBefore(LocalDateTime.now().plusMinutes(1));
    }

    @Test
    void shouldGetPublishedPostById() {
        // Given - create a published post
        Post post = Post.builder()
                .title("Published Post")
                .content("Content of published post")
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);

        // When
        ResponseEntity<PostResponse> response = restTemplate.getForEntity(
                "/posts/{id}", PostResponse.class, post.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Published Post");
        assertThat(response.getBody().getContent()).isEqualTo("Content of published post");
        assertThat(response.getBody().getIsPublished()).isTrue();
        assertThat(response.getBody().getAuthorUsername()).isEqualTo(testUsername);

        // Verify view count was incremented
        Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(1L);
    }

    @Test
    void shouldNotGetUnpublishedPost() {
        // Given - create an unpublished post
        Post post = Post.builder()
                .title("Draft Post")
                .content("Content of draft post")
                .isPublished(false)
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);

        // When
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/posts/{id}", Object.class, post.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldValidatePostCreationRequest() {
        // Given - invalid request
        CreatePostRequest invalidRequest = CreatePostRequest.builder()
                .title("") // empty title
                .content("") // empty content
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Author-Username", testUsername);
        HttpEntity<CreatePostRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                "/posts", HttpMethod.POST, entity, Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("validationErrors");
    }

    @Test
    void shouldFailWithNonExistentAuthor() {
        // Given
        CreatePostRequest request = CreatePostRequest.builder()
                .title("Test Post")
                .content("Test content")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Author-Username", "nonexistent");
        HttpEntity<CreatePostRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Object> response = restTemplate.exchange(
                "/posts", HttpMethod.POST, entity, Object.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldGetPostsByUser() {
        // Given - create multiple posts
        Post post1 = Post.builder()
                .title("First Post")
                .content("First content")
                .isPublished(true)
                .publishedAt(LocalDateTime.now().minusDays(1))
                .author(testUser)
                .viewsCount(0L)
                .build();

        Post post2 = Post.builder()
                .title("Second Post")
                .content("Second content")
                .isPublished(false)
                .author(testUser)
                .viewsCount(0L)
                .build();

        postRepository.save(post1);
        postRepository.save(post2);

        // When
        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                "/posts/author/{username}", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PostSummaryResponse>>() {}, testUsername);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Second Post"); // Most recent first
        assertThat(response.getBody().get(1).getTitle()).isEqualTo("First Post");
    }
}