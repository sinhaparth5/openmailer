package com.openmailer.openmailer.repository;

import com.openmailer.openmailer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

  /**
   * Find user by email address.
   *
   * @param email the email address
   * @return Optional containing the user if found
   */
  Optional<User> findByEmail(String email);

  /**
   * Find user by username.
   *
   * @param username the username
   * @return Optional containing the user if found
   */
  Optional<User> findByUsername(String username);

  /**
   * Check if a user exists with the given email.
   *
   * @param email the email address
   * @return true if user exists, false otherwise
   */
  boolean existsByEmail(String email);

  /**
   * Check if a user exists with the given username.
   *
   * @param username the username
   * @return true if user exists, false otherwise
   */
  boolean existsByUsername(String username);
}
