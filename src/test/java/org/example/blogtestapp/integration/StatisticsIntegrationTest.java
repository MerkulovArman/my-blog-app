package org.example.blogtestapp.integration;

import org.example.blogtestapp.dto.ActiveUserStatisticsResponse;
import org.example.blogtestapp.dto.TopicStatisticsResponse;
import org.example.blogtestapp.entity.Post;
import org.example.blogtestapp.entity.Tag;
import org.example.blogtestapp.entity.User;
import org.example.blogtestapp.entity.Comment;
import org.example.blogtestapp.entity.Like;
import org.example.blogtestapp.repository.TagRepository;
import org.example.blogtestapp.repository.CommentRepository;
import org.example.blogtestapp.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для статистики и группировки данных
 */
class StatisticsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    private User testUser1;
    private User testUser2;
    private Tag javaTag;
    private Tag springTag;
    private Tag testingTag;

    @BeforeEach
    void setUp() {
        // Clean all data
        likeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser1 = User.builder()
                .username("activeuser1")
                .email("active1@example.com")
                .displayName("Active User 1")
                .isActive(true)
                .build();
        testUser1 = userRepository.save(testUser1);

        testUser2 = User.builder()
                .username("activeuser2")
                .email("active2@example.com")
                .displayName("Active User 2")
                .isActive(true)
                .build();
        testUser2 = userRepository.save(testUser2);

        // Create test tags
        javaTag = Tag.builder()
                .name("java")
                .usageCount(0L)
                .isActive(true)
                .build();
        javaTag = tagRepository.save(javaTag);

        springTag = Tag.builder()
                .name("spring")
                .usageCount(0L)
                .isActive(true)
                .build();
        springTag = tagRepository.save(springTag);

        testingTag = Tag.builder()
                .name("testing")
                .usageCount(0L)
                .isActive(true)
                .build();
        testingTag = tagRepository.save(testingTag);

        // Create test posts with different tags
        createTestPost("Java Basics", "Learning Java fundamentals", testUser1, Set.of(javaTag), 100L);
        createTestPost("Spring Framework", "Advanced Spring concepts", testUser1, Set.of(javaTag, springTag), 150L);
        createTestPost("Unit Testing", "Writing effective unit tests", testUser2, Set.of(testingTag, javaTag), 75L);
        createTestPost("Spring Boot", "Getting started with Spring Boot", testUser2, Set.of(springTag), 200L);
        
        // Add some comments and likes for activity testing
        addCommentsAndLikes();
    }

    private void createTestPost(String title, String content, User author, Set<Tag> tags, Long viewsCount) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .isPublished(true)
                .publishedAt(LocalDateTime.now().minusDays(2)) // Within last 10 days
                .author(author)
                .viewsCount(viewsCount)
                .build();
        post = postRepository.save(post);
        
        // Refresh tags from database to avoid detached entity issues
        Set<Tag> managedTags = new HashSet<>();
        for (Tag tag : tags) {
            Tag managedTag = tagRepository.findById(tag.getId()).orElse(tag);
            managedTags.add(managedTag);
        }
        post.setTags(managedTags);
        postRepository.save(post);
    }

    private void addCommentsAndLikes() {
        List<Post> posts = postRepository.findAll();
        
        for (Post post : posts) {
            // Add comments
            Comment comment1 = Comment.builder()
                    .content("Great post!")
                    .post(post)
                    .author(testUser1)
                    .build();
            commentRepository.save(comment1);

            Comment comment2 = Comment.builder()
                    .content("Very helpful!")
                    .post(post)
                    .author(testUser2)
                    .build();
            commentRepository.save(comment2);

            // Add likes
            Like like1 = Like.builder()
                    .post(post)
                    .user(testUser1)
                    .build();
            likeRepository.save(like1);

            Like like2 = Like.builder()
                    .post(post)
                    .user(testUser2)
                    .build();
            likeRepository.save(like2);
        }
    }

    @Test
    void shouldGetTopicStatistics() {
        // When
        ResponseEntity<List<TopicStatisticsResponse>> response = restTemplate.exchange(
                "/posts/statistics/topics", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TopicStatisticsResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3); // java, spring, testing

        // Find java tag statistics
        TopicStatisticsResponse javaStats = response.getBody().stream()
                .filter(stat -> "java".equals(stat.getTopic()))
                .findFirst()
                .orElseThrow();

        assertThat(javaStats.getPostsCount()).isEqualTo(3L); // 3 posts with java tag
        assertThat(javaStats.getTotalViews()).isEqualTo(325L); // 100 + 150 + 75

        // Find spring tag statistics
        TopicStatisticsResponse springStats = response.getBody().stream()
                .filter(stat -> "spring".equals(stat.getTopic()))
                .findFirst()
                .orElseThrow();

        assertThat(springStats.getPostsCount()).isEqualTo(2L); // 2 posts with spring tag
        assertThat(springStats.getTotalViews()).isEqualTo(350L); // 150 + 200
    }

    @Test
    void shouldGetActiveUserStatistics() {
        // When
        ResponseEntity<List<ActiveUserStatisticsResponse>> response = restTemplate.exchange(
                "/posts/statistics/active-users", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ActiveUserStatisticsResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2); // 2 active users

        // Check that statistics are ordered by activity score
        ActiveUserStatisticsResponse mostActive = response.getBody().get(0);
        ActiveUserStatisticsResponse secondActive = response.getBody().get(1);

        assertThat(mostActive.getActivityScore()).isGreaterThanOrEqualTo(secondActive.getActivityScore());

        // Verify user statistics contain expected data
        assertThat(mostActive.getUsername()).isIn("activeuser1", "activeuser2");
        assertThat(mostActive.getPostsCount()).isGreaterThan(0L);
        assertThat(mostActive.getCommentsCount()).isGreaterThan(0L);
        assertThat(mostActive.getTotalViews()).isGreaterThan(0L);
    }

    @Test
    void shouldReturnEmptyTopicStatisticsForInactiveTags() {
        // Given - make all tags inactive
        tagRepository.findAll().forEach(tag -> {
            tag.setIsActive(false);
            tagRepository.save(tag);
        });

        // When
        ResponseEntity<List<TopicStatisticsResponse>> response = restTemplate.exchange(
                "/posts/statistics/topics", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TopicStatisticsResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnEmptyActiveUserStatisticsForInactiveUsers() {
        // Given - make all users inactive
        userRepository.findAll().forEach(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });

        // When
        ResponseEntity<List<ActiveUserStatisticsResponse>> response = restTemplate.exchange(
                "/posts/statistics/active-users", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ActiveUserStatisticsResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldCalculateActivityScoreCorrectly() {
        // When
        ResponseEntity<List<ActiveUserStatisticsResponse>> response = restTemplate.exchange(
                "/posts/statistics/active-users", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ActiveUserStatisticsResponse>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        for (ActiveUserStatisticsResponse userStats : response.getBody()) {
            // Activity score = posts * 10 + comments * 3 + likes * 1
            double expectedScore = userStats.getPostsCount() * 10.0 + 
                                 userStats.getCommentsCount() * 3.0 + 
                                 userStats.getLikesReceived() * 1.0;
            
            // Allow for small floating point differences and view count bonus
            assertThat(userStats.getActivityScore()).isGreaterThanOrEqualTo(expectedScore);
        }
    }
}