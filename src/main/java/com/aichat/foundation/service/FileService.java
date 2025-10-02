package com.aichat.foundation.service;

import com.aichat.foundation.dto.FileAttachmentDto;
import com.aichat.foundation.entity.FileAttachment;
import com.aichat.foundation.entity.Message;
import com.aichat.foundation.exception.FileStorageException;
import com.aichat.foundation.repository.FileAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Value("${file.max-size}")
    private long maxFileSize;
    
    @Value("${file.allowed-types}")
    private String allowedTypes;
    
    private final FileAttachmentRepository fileAttachmentRepository;
    
    public FileService(FileAttachmentRepository fileAttachmentRepository) {
        this.fileAttachmentRepository = fileAttachmentRepository;
    }
    
    /**
     * Upload and store a file
     */
    public FileAttachmentDto uploadFile(MultipartFile file, Message message) {
        validateFile(file);
        
        try {
            // Ensure upload directory exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file to disk
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create file attachment entity
            FileAttachment fileAttachment = new FileAttachment(
                originalFilename,
                file.getContentType(),
                filePath.toString(),
                file.getSize(),
                message
            );
            
            // Save to database
            FileAttachment savedAttachment = fileAttachmentRepository.save(fileAttachment);
            
            // Convert to DTO
            return convertToDto(savedAttachment);
            
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }
    
    /**
     * Get file content as byte array
     */
    public byte[] getFileContent(UUID fileId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(fileId)
            .orElseThrow(() -> new FileStorageException("File not found with id: " + fileId));
        
        try {
            Path filePath = Paths.get(fileAttachment.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file: " + fileAttachment.getFileName(), e);
        }
    }
    
    /**
     * Delete a file
     */
    public void deleteFile(UUID fileId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(fileId)
            .orElseThrow(() -> new FileStorageException("File not found with id: " + fileId));
        
        try {
            // Delete from file system
            Path filePath = Paths.get(fileAttachment.getFilePath());
            Files.deleteIfExists(filePath);
            
            // Delete from database
            fileAttachmentRepository.delete(fileAttachment);
            
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file: " + fileAttachment.getFileName(), e);
        }
    }
    
    /**
     * Get file attachments for a message
     */
    public List<FileAttachmentDto> getFileAttachmentsByMessageId(UUID messageId) {
        List<FileAttachment> attachments = fileAttachmentRepository.findByMessageIdOrderByUploadedAtAsc(messageId);
        return attachments.stream()
            .map(this::convertToDto)
            .toList();
    }
    
    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File size exceeds maximum limit of " + maxFileSize + " bytes");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new FileStorageException("File type not allowed. Allowed types: " + allowedTypes);
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new FileStorageException("Invalid filename: " + filename);
        }
    }
    
    /**
     * Check if content type is allowed
     */
    private boolean isAllowedContentType(String contentType) {
        List<String> allowedTypesList = Arrays.asList(allowedTypes.split(","));
        return allowedTypesList.contains(contentType.trim());
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
    
    /**
     * Convert FileAttachment entity to DTO
     */
    private FileAttachmentDto convertToDto(FileAttachment fileAttachment) {
        return new FileAttachmentDto(
            fileAttachment.getId(),
            fileAttachment.getMessage().getId(),
            fileAttachment.getFileName(),
            fileAttachment.getFileType(),
            fileAttachment.getFilePath(),
            fileAttachment.getFileSize(),
            fileAttachment.getUploadedAt()
        );
    }
    
    /**
     * Check if file exists
     */
    public boolean fileExists(UUID fileId) {
        return fileAttachmentRepository.existsById(fileId);
    }
    
    /**
     * Get file info without content
     */
    public FileAttachmentDto getFileInfo(UUID fileId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(fileId)
            .orElseThrow(() -> new FileStorageException("File not found with id: " + fileId));
        
        return convertToDto(fileAttachment);
    }
    
    /**
     * Calculate total file size for a session
     */
    public long getTotalFileSizeBySessionId(UUID sessionId) {
        return fileAttachmentRepository.calculateTotalFileSizeBySessionId(sessionId);
    }
    
    /**
     * Clean up old files (for maintenance)
     */
    public void cleanupOldFiles(int daysOld) {
        // Implementation for cleanup based on business requirements
        // This would typically run as a scheduled task
    }
}