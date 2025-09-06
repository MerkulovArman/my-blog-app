package org.example.blogtestapp.integration;

import org.example.blogtestapp.TestcontainersConfiguration;
import org.example.blogtestapp.dto.CreateUserRequest;
import org.example.blogtestapp.dto.UserResponse;
import org.example.blogtestapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для UserController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .bio("Test bio")
                .build();

        // When
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/users", request, UserResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().getDisplayName()).isEqualTo("Test User");
        assertThat(response.getBody().getBio()).isEqualTo("Test bio");
        assertThat(response.getBody().getIsActive()).isTrue();

        // Verify user was saved to database
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByUsername("testuser")).isPresent();
    }

    @Test
    void shouldNotCreateUserWithDuplicateUsername() {
        // Given
        CreateUserRequest firstRequest = CreateUserRequest.builder()
                .username("testuser")
                .email("test1@example.com")
                .build();

        CreateUserRequest secondRequest = CreateUserRequest.builder()
                .username("testuser")
                .email("test2@example.com")
                .build();

        // When - create first user
        ResponseEntity<UserResponse> firstResponse = restTemplate.postForEntity(
                "/users", firstRequest, UserResponse.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Then - second user creation should fail
        ResponseEntity<Object> secondResponse = restTemplate.postForEntity(
                "/users", secondRequest, Object.class);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify only one user was created
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldGetUserById() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .build();

        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
                "/users", request, UserResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse createdUser = createResponse.getBody();
        assertThat(createdUser).isNotNull();

        // When
        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(
                "/users/{id}", UserResponse.class, createdUser.getId());

        // Then
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(createdUser.getId());
        assertThat(getResponse.getBody().getUsername()).isEqualTo("testuser");
        assertThat(getResponse.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        // When
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/users/{id}", Object.class, 999L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldValidateUserCreationRequest() {
        // Given - invalid request (missing required fields)
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .username("") // empty username
                .email("invalid-email") // invalid email format
                .build();

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/users", invalidRequest, Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("validationErrors");
    }
}