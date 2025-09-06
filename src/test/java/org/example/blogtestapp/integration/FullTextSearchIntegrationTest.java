package org.example.blogtestapp.integration;

import org.example.blogtestapp.TestcontainersConfiguration;
import org.example.blogtestapp.dto.PostSummaryResponse;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.User;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для полнотекстового поиска
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class FullTextSearchIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

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

        // Create test posts with different content
        createTestPost("Путешествие в Италию", 
                "Моя поездка в прекрасную Италию была незабываемой. Рим, Венеция, Флоренция - все эти города оставили глубокое впечатление.");

        createTestPost("Изучение Java Spring Framework",
                "Spring Framework является мощным инструментом для разработки Java приложений. Особенно полезен Spring Boot для быстрого старта проектов.");

        createTestPost("Рецепты итальянской кухни",
                "Итальянская кухня славится своими пастами и пиццами. Аутентичные рецепты передаются из поколения в поколение в итальянских семьях.");

        createTestPost("Современные технологии программирования",
                "В современном мире программирования важно знать различные технологии: Java, Python, JavaScript, React, Angular и многие другие.");

        createTestPost("Гастрономический тур по Европе",
                "Европейская кухня очень разнообразна. От французских круассанов до немецких сосисок, от итальянской пасты до испанской паэльи.");
    }

    private void createTestPost(String title, String content) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .isPublished(true)
                .publishedAt(LocalDateTime.now())
                .author(testUser)
                .viewsCount(0L)
                .build();
        postRepository.save(post);
    }

    @Test
    void shouldPerformFullTextSearch() {
        // Test search for "Италия" - should find posts about Italy
        String url = UriComponentsBuilder.fromPath("/posts/search/fulltext")
                .queryParam("q", "Италия")
                .toUriString();

        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PostSummaryResponse>>() {});

        System.out.println("response = " + response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1); // Should find 1 post about Italy
        assertThat(response.getBody().get(0).getTitle()).isNotNull();
        assertThat(response.getBody().get(0).getExcerpt()).isNotNull();
    }

    @Test
    void shouldSearchForJavaContent() {
        // Test search for "Java" - should find programming-related posts
        String url = UriComponentsBuilder.fromPath("/posts/search/fulltext")
                .queryParam("q", "Java")
                .toUriString();

        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PostSummaryResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2); // Should find Java-related posts
    }

    @Test
    void shouldSearchForCookingContent() {
        // Test search for "кухня" (cuisine) - should find cooking-related posts
        String url = UriComponentsBuilder.fromPath("/posts/search/fulltext")
                .queryParam("q", "кухня")
                .toUriString();

        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PostSummaryResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2); // Should find cooking-related posts
    }

    @Test
    void shouldReturnEmptyForNonExistentSearch() {
        // Test search for non-existent content
        String url = UriComponentsBuilder.fromPath("/posts/search/fulltext")
                .queryParam("q", "несуществующиеслова")
                .toUriString();

        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PostSummaryResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldPerformSimpleSearch() {
        // Test simple LIKE-based search
        String url = UriComponentsBuilder.fromPath("/posts/search")
                .queryParam("q", "Spring")
                .toUriString();

        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PostSummaryResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1); // Should find Spring-related post
    }

    @Test
    void shouldSearchInBothTitleAndContent() {
        // Test search that should match both title and content
        String url = UriComponentsBuilder.fromPath("/posts/search")
                .queryParam("q", "программирования")
                .toUriString();

        ResponseEntity<List<PostSummaryResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PostSummaryResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1); // Should find programming post
    }

    @Test
    void shouldTestRepositoryFullTextSearchDirectly() {
        // Test the repository method directly
        List<Post> javaResults = postRepository.fullTextSearch("Java");
        assertThat(javaResults).hasSize(2);

        List<Post> italyResults = postRepository.fullTextSearch("Италия");
        assertThat(italyResults).hasSize(1);

        List<Post> cookingResults = postRepository.fullTextSearch("кухня");
        assertThat(cookingResults).hasSize(2);

        List<Post> noResults = postRepository.fullTextSearch("несуществующиеслова");
        assertThat(noResults).isEmpty();
    }

    @Test
    void shouldTestSimpleSearchRepository() {
        // Test simple search repository method
        List<Post> springResults = postRepository.findByTitleOrContentContaining("Spring");
        assertThat(springResults).hasSize(1);

        List<Post> italyResults = postRepository.findByTitleOrContentContaining("Италию");
        assertThat(italyResults).hasSize(1);

        List<Post> techResults = postRepository.findByTitleOrContentContaining("технологии");
        assertThat(techResults).hasSize(1);
    }

    @Test
    void shouldVerifySearchVectorIsGenerated() {
        // Verify that search_vector field is populated by the trigger
        List<Post> allPosts = postRepository.findAll();
        assertThat(allPosts).hasSize(5);
        
        // All posts should have search_vector populated by the trigger
        // We can't directly check the tsvector content in Java, but we can verify the search works
        List<Post> searchResults = postRepository.fullTextSearch("путешествие");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getTitle()).contains("Путешествие");
    }
}