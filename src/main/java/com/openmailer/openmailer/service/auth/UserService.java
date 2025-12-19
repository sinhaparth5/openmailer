package com.openmailer.openmailer.service.auth;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service class for User management operations.
 * Handles user CRUD operations with validation.
 */
@Service
@Transactional
public class UserService {

  private final UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Create a new user.
   *
   * @param user the user to create
   * @return the created user
   * @throws ValidationException if email or username already exists
   */
  public User createUser(User user) {
    // Validate email uniqueness
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new ValidationException("Email already exists", "email");
    }

    // Validate username uniqueness
    if (userRepository.existsByUsername(user.getUsername())) {
      throw new ValidationException("Username already exists", "username");
    }

    // Set timestamps
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    return userRepository.save(user);
  }

  /**
   * Find user by ID.
   *
   * @param id the user ID
   * @return the user
   * @throws ResourceNotFoundException if user not found
   */
  @Transactional(readOnly = true)
  public User findById(String id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
  }

  /**
   * Find user by email.
   *
   * @param email the email address
   * @return Optional containing the user if found
   */
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  /**
   * Find user by email or throw exception.
   *
   * @param email the email address
   * @return the user
   * @throws ResourceNotFoundException if user not found
   */
  @Transactional(readOnly = true)
  public User findByEmailOrThrow(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
  }

  /**
   * Find user by username.
   *
   * @param username the username
   * @return Optional containing the user if found
   */
  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  /**
   * Update user information.
   *
   * @param id the user ID
   * @param updatedUser the updated user data
   * @return the updated user
   * @throws ResourceNotFoundException if user not found
   */
  public User updateUser(String id, User updatedUser) {
    User user = findById(id);

    // Update allowed fields
    if (updatedUser.getUsername() != null && !updatedUser.getUsername().equals(user.getUsername())) {
      if (userRepository.existsByUsername(updatedUser.getUsername())) {
        throw new ValidationException("Username already exists", "username");
      }
      user.setUsername(updatedUser.getUsername());
    }

    if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(updatedUser.getEmail())) {
        throw new ValidationException("Email already exists", "email");
      }
      user.setEmail(updatedUser.getEmail());
    }

    if (updatedUser.getFirstName() != null) {
      user.setFirstName(updatedUser.getFirstName());
    }

    if (updatedUser.getLastName() != null) {
      user.setLastName(updatedUser.getLastName());
    }

    user.setUpdatedAt(LocalDateTime.now());

    return userRepository.save(user);
  }

  /**
   * Delete user (GDPR compliance).
   *
   * @param id the user ID
   * @throws ResourceNotFoundException if user not found
   */
  public void deleteUser(String id) {
    User user = findById(id);
    userRepository.delete(user);
  }

  /**
   * Check if email exists.
   *
   * @param email the email address
   * @return true if exists, false otherwise
   */
  @Transactional(readOnly = true)
  public boolean emailExists(String email) {
    return userRepository.existsByEmail(email);
  }

  /**
   * Check if username exists.
   *
   * @param username the username
   * @return true if exists, false otherwise
   */
  @Transactional(readOnly = true)
  public boolean usernameExists(String username) {
    return userRepository.existsByUsername(username);
  }
}
