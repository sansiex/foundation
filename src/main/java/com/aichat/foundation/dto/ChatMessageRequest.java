package com.aichat.foundation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ChatMessageRequest {
    
    @NotNull(message = "Session ID is required")
    private UUID sessionId;
    
    @NotBlank(message = "Content cannot be empty")
    @Size(max = 4000, message = "Message content must not exceed 4000 characters")
    private String content;
    
    private String type = "text";  // Default to text
    private String clientId;       // For client-side tracking
    
    // Constructors
    public ChatMessageRequest() {}
    
    public ChatMessageRequest(UUID sessionId, String content, String type) {
        this.sessionId = sessionId;
        this.content = content;
        this.type = type;
    }
    
    // Getters and Setters
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
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}