package com.aichat.foundation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MessageDto {
    
    private UUID id;
    
    @NotNull(message = "Session ID is required")
    private UUID sessionId;
    
    @NotBlank(message = "Content cannot be empty")
    @Size(max = 4000, message = "Message content must not exceed 4000 characters")
    private String content;
    
    @NotBlank(message = "Message type is required")
    private String type;
    
    private LocalDateTime createdAt;
    private String metadata;
    private List<FileAttachmentDto> attachments;
    
    // Constructors
    public MessageDto() {}
    
    public MessageDto(UUID id, UUID sessionId, String content, String type, 
                     LocalDateTime createdAt, String metadata, List<FileAttachmentDto> attachments) {
        this.id = id;
        this.sessionId = sessionId;
        this.content = content;
        this.type = type;
        this.createdAt = createdAt;
        this.metadata = metadata;
        this.attachments = attachments;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public List<FileAttachmentDto> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<FileAttachmentDto> attachments) {
        this.attachments = attachments;
    }
}