package com.aichat.foundation.repository;

import com.aichat.foundation.entity.ChatSession;
import com.aichat.foundation.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    
    /**
     * Find all chat sessions for a specific user, ordered by updated date descending
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    /**
     * Find active chat sessions for a specific user
     */
    List<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, SessionStatus status);
    
    /**
     * Find a specific session by ID and user ID for security
     */
    Optional<ChatSession> findByIdAndUserId(UUID id, String userId);
    
    /**
     * Count total sessions for a user
     */
    long countByUserId(String userId);
    
    /**
     * Find sessions created within a specific time range
     */
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<ChatSession> findByUserIdAndCreatedAtBetween(@Param("userId") String userId, 
                                                       @Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find sessions that haven't been updated for a specified duration (for cleanup)
     */
    @Query("SELECT s FROM ChatSession s WHERE s.updatedAt < :cutoffDate")
    List<ChatSession> findInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete sessions older than specified date
     */
    void deleteByUpdatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * Check if user owns the session
     */
    boolean existsByIdAndUserId(UUID sessionId, String userId);
}