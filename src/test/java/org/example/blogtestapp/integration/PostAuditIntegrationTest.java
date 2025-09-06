package org.example.blogtestapp.integration;

import org.example.blogtestapp.TestcontainersConfiguration;
import org.example.blogtestapp.dto.PostAuditResponse;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.User;
import org.example.blogtestapp.repository.PostAuditLogRepository;
import org.example.blogtestapp.repository.PostRepository;
import org.example.blogtestapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для PostAuditController и функции аудита
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PostAuditIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostAuditLogRepository postAuditLogRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testauthor")
                .email("author@example.com")
                .displayName("Test Author")
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);
        postAuditLogRepository.deleteAll();
    }

    @Test
    void shouldCreateAuditLogOnPostInsert() {
        // Given - no audit logs initially
        assertThat(postAuditLogRepository.count()).isEqualTo(0);

        // When - create a new post
        Post post = Post.builder()
                .title("Test Post for Audit")
                .content("Content for audit test")
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                .viewsCount(0L)
                .build();
        postRepository.save(post);

        // Then - audit log should be created
        assertThat(postAuditLogRepository.count()).isEqualTo(1);
        
        var auditLog = postAuditLogRepository.findAll().get(0);
        assertThat(auditLog.getOperation()).isEqualTo("INSERT");
        assertThat(auditLog.getPostId()).isEqualTo(post.getId());
        assertThat(auditLog.getNewTitle()).isEqualTo("Test Post for Audit");
        assertThat(auditLog.getNewContent()).isEqualTo("Content for audit test");
        assertThat(auditLog.getNewIsPublished()).isTrue();
    }

    @Test
    void shouldCreateAuditLogOnPostUpdate() {
        // Given - create initial post
        Post post = Post.builder()
                .title("Original Title")
                .content("Original content")
                .isPublished(false)
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);

        // Clear any INSERT audit logs
        postAuditLogRepository.deleteAll();

        // When - update the post
        post.setTitle("Updated Title");
        post.setContent("Updated content");
        post.setIsPublished(true);
        postRepository.save(post);

        // Then - UPDATE audit log should be created
        assertThat(postAuditLogRepository.count()).isEqualTo(1);
        
        var auditLog = postAuditLogRepository.findAll().get(0);
        assertThat(auditLog.getOperation()).isEqualTo("UPDATE");
        assertThat(auditLog.getPostId()).isEqualTo(post.getId());
        assertThat(auditLog.getOldTitle()).isEqualTo("Original Title");
        assertThat(auditLog.getNewTitle()).isEqualTo("Updated Title");
        assertThat(auditLog.getOldContent()).isEqualTo("Original content");
        assertThat(auditLog.getNewContent()).isEqualTo("Updated content");
        assertThat(auditLog.getOldIsPublished()).isFalse();
        assertThat(auditLog.getNewIsPublished()).isTrue();
    }

    @Test
    void shouldCreateAuditLogOnPostDelete() {
        // Given - create initial post
        Post post = Post.builder()
                .title("Post to Delete")
                .content("Content to delete")
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);
        Long postId = post.getId();

        // Clear INSERT audit log
        postAuditLogRepository.deleteAll();

        // When - delete the post
        postRepository.delete(post);

        // Then - DELETE audit log should be created
        assertThat(postAuditLogRepository.count()).isEqualTo(1);
        
        var auditLog = postAuditLogRepository.findAll().get(0);
        assertThat(auditLog.getOperation()).isEqualTo("DELETE");
        assertThat(auditLog.getPostId()).isEqualTo(postId);
        assertThat(auditLog.getOldTitle()).isEqualTo("Post to Delete");
        assertThat(auditLog.getOldContent()).isEqualTo("Content to delete");
        assertThat(auditLog.getOldIsPublished()).isTrue();
    }

    @Test
    void shouldGetPostAuditHistory() {
        // Given - create post and perform some operations
        Post post = Post.builder()
                .title("Test Post")
                .content("Original content")
                .isPublished(false)
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);
        
        // Update the post
        post.setTitle("Updated Title");
        postRepository.save(post);
        
        // Update again
        post.setIsPublished(true);
        postRepository.save(post);

        // When
        ResponseEntity<List<PostAuditResponse>> response = restTemplate.exchange(
                "/private/audit-info/post/{postId}", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PostAuditResponse>>() {}, post.getId());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3); // INSERT + 2 UPDATEs
        assertThat(response.getBody().get(0).getOperation()).isEqualTo("UPDATE"); // Most recent first
        assertThat(response.getBody().get(1).getOperation()).isEqualTo("UPDATE");
        assertThat(response.getBody().get(2).getOperation()).isEqualTo("INSERT");
    }

    @Test
    void shouldGetDeletedPosts() {
        // Given - create and delete a post
        Post post = Post.builder()
                .title("Post to Delete")
                .content("Content")
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);
        Long postId = post.getId();
        
        postRepository.delete(post);

        // When
        ResponseEntity<List<PostAuditResponse>> response = restTemplate.exchange(
                "/private/audit-info/deleted-posts", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PostAuditResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getOperation()).isEqualTo("DELETE");
        assertThat(response.getBody().get(0).getPostId()).isEqualTo(postId);
        assertThat(response.getBody().get(0).getOldTitle()).isEqualTo("Post to Delete");
    }

    @Test
    void shouldGetTitleChanges() {
        // Given - create post and change title
        Post post = Post.builder()
                .title("Original Title")
                .content("Content")
                .isPublished(false)
                .author(testUser)
                .viewsCount(0L)
                .build();
        post = postRepository.save(post);
        
        // Change title
        post.setTitle("New Title");
        postRepository.save(post);

        // When
        ResponseEntity<List<PostAuditResponse>> response = restTemplate.exchange(
                "/private/audit-info/title-changes", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PostAuditResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getOperation()).isEqualTo("UPDATE");
        assertThat(response.getBody().get(0).getOldTitle()).isEqualTo("Original Title");
        assertThat(response.getBody().get(0).getNewTitle()).isEqualTo("New Title");
    }

    @Test
    void shouldGetRecentChanges() {
        // Given - create multiple posts with changes
        for (int i = 1; i <= 3; i++) {
            Post post = Post.builder()
                    .title("Post " + i)
                    .content("Content " + i)
                    .isPublished(false)
                    .author(testUser)
                    .viewsCount(0L)
                    .build();
            postRepository.save(post);
        }

        // When
        String url = UriComponentsBuilder.fromPath("/private/audit-info/recent")
                .queryParam("limit", "10")
                .toUriString();

        ResponseEntity<List<PostAuditResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PostAuditResponse>>() {});

        // Then
        System.out.println("response = " + response.getBody().size());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3); // 3 INSERT operations
    }
}