package com.aichat.foundation.repository;

import com.aichat.foundation.entity.Message;
import com.aichat.foundation.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    /**
     * Find all messages in a session, ordered by creation time
     */
    List<Message> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    
    /**
     * Find messages by type in a session
     */
    List<Message> findBySessionIdAndTypeOrderByCreatedAtAsc(UUID sessionId, MessageType type);
    
    /**
     * Find recent messages in a session (limit for performance)
     */
    @Query("SELECT m FROM Message m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<Message> findRecentMessagesBySessionId(@Param("sessionId") UUID sessionId);
    
    /**
     * Count messages in a session
     */
    long countBySessionId(UUID sessionId);
    
    /**
     * Find messages created after a specific date in a session
     */
    List<Message> findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID sessionId, LocalDateTime since);
    
    /**
     * Find the last message in a session
     */
    @Query("SELECT m FROM Message m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<Message> findLastMessageBySessionId(@Param("sessionId") UUID sessionId);
    
    /**
     * Find messages with attachments
     */
    @Query("SELECT DISTINCT m FROM Message m LEFT JOIN FETCH m.attachments WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<Message> findMessagesWithAttachmentsBySessionId(@Param("sessionId") UUID sessionId);
    
    /**
     * Delete all messages for a session (cascade should handle this, but explicit for cleanup)
     */
    void deleteBySessionId(UUID sessionId);
    
    /**
     * Find messages older than specified date for cleanup
     */
    @Query("SELECT m FROM Message m WHERE m.createdAt < :cutoffDate")
    List<Message> findOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);
}