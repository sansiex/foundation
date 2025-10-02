package com.aichat.foundation.repository;

import com.aichat.foundation.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {
    
    /**
     * Find all attachments for a specific message
     */
    List<FileAttachment> findByMessageIdOrderByUploadedAtAsc(UUID messageId);
    
    /**
     * Find all attachments in a session (through messages)
     */
    @Query("SELECT f FROM FileAttachment f WHERE f.message.session.id = :sessionId ORDER BY f.uploadedAt ASC")
    List<FileAttachment> findBySessionId(@Param("sessionId") UUID sessionId);
    
    /**
     * Find attachment by file path (for uniqueness check)
     */
    Optional<FileAttachment> findByFilePath(String filePath);
    
    /**
     * Find attachments by file type
     */
    List<FileAttachment> findByFileTypeOrderByUploadedAtDesc(String fileType);
    
    /**
     * Find attachments uploaded within date range
     */
    List<FileAttachment> findByUploadedAtBetweenOrderByUploadedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Calculate total file size for a session
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileAttachment f WHERE f.message.session.id = :sessionId")
    Long calculateTotalFileSizeBySessionId(@Param("sessionId") UUID sessionId);
    
    /**
     * Calculate total file size for a user (through sessions)
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileAttachment f WHERE f.message.session.userId = :userId")
    Long calculateTotalFileSizeByUserId(@Param("userId") String userId);
    
    /**
     * Find files older than specified date for cleanup
     */
    @Query("SELECT f FROM FileAttachment f WHERE f.uploadedAt < :cutoffDate")
    List<FileAttachment> findOldFiles(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count attachments for a message
     */
    long countByMessageId(UUID messageId);
    
    /**
     * Find large files above size threshold
     */
    @Query("SELECT f FROM FileAttachment f WHERE f.fileSize > :sizeThreshold ORDER BY f.fileSize DESC")
    List<FileAttachment> findLargeFiles(@Param("sizeThreshold") Long sizeThreshold);
    
    /**
     * Delete attachments for a specific message
     */
    void deleteByMessageId(UUID messageId);
}