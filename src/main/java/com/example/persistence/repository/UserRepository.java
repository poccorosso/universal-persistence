package com.example.persistence.repository;

import com.example.persistence.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository example
 */
@Repository
public interface UserRepository extends BaseRepository<User, Long> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUidAndDeletedFalse(String uid);

    /**
     * Find user by email
     */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /**
     * Custom JPQL query
     */
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:name% AND  u.deleted = false")
    List<User> findByFullNameContainingAndStatus(@Param("name") String name);
    
    /**
     * Native SQL query example
     */
    @Query(value = "SELECT * FROM users WHERE age > :age AND deleted = false ORDER BY created_at DESC", 
           nativeQuery = true)
    List<User> findUsersOlderThan(@Param("age") Integer age);
    
    /**
     * Check if uid exists
     */
    boolean existsByUidAndDeletedFalse(String uid);
    
    /**
     * Check if email exists
     */
    boolean existsByEmailAndDeletedFalse(String email);
}