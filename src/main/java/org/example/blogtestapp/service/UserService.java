package org.example.blogtestapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.blogtestapp.dto.CreateUserRequest;
import org.example.blogtestapp.dto.UpdateUserRequest;
import org.example.blogtestapp.dto.UserResponse;
import org.example.blogtestapp.entity.User;
import org.example.blogtestapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с пользователями
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Создать нового пользователя
     */
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());

        // Проверка уникальности username и email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .bio(request.getBio())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    /**
     * Получить пользователя по ID
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToUserResponse);
    }

    /**
     * Получить пользователя по username
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserResponse);
    }

    /**
     * Получить всех активных пользователей
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Обновить пользователя
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        User savedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    /**
     * Деактивировать пользователя
     */
    public void deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated successfully with ID: {}", id);
    }

    /**
     * Поиск пользователей
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String searchTerm) {
        return userRepository.findBySearchTerm(searchTerm)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить топ пользователей по количеству постов
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getTopUsersByPostCount() {
        return userRepository.findTopUsersByPostCount()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Маппинг User в UserResponse
     */
    private UserResponse mapToUserResponse(User user) {
        Long postsCount = user.getPosts() != null ? 
            user.getPosts().stream()
                .filter(post -> post.getIsPublished())
                .count() : 0L;
        
        Long commentsCount = user.getComments() != null ? 
            user.getComments().stream()
                .filter(comment -> !comment.getIsDeleted())
                .count() : 0L;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .isActive(user.getIsActive())
                .postsCount(postsCount)
                .commentsCount(commentsCount)
                .build();
    }
}