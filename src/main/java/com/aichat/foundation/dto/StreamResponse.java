package com.aichat.foundation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class StreamResponse {
    
    private String type;           // stream_start, stream_chunk, stream_end, error
    private UUID sessionId;
    private UUID messageId;
    private String content;
    private StreamMetadata metadata;
    
    // Constructors
    public StreamResponse() {}
    
    public StreamResponse(String type, UUID sessionId, UUID messageId, String content) {
        this.type = type;
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.content = content;
        this.metadata = new StreamMetadata();
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }
    
    public UUID getMessageId() {
        return messageId;
    }
    
    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public StreamMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(StreamMetadata metadata) {
        this.metadata = metadata;
    }
    
    // Static factory methods for different response types
    public static StreamResponse streamStart(UUID sessionId, UUID messageId) {
        return new StreamResponse("stream_start", sessionId, messageId, "");
    }
    
    public static StreamResponse streamChunk(UUID sessionId, UUID messageId, String content) {
        return new StreamResponse("stream_chunk", sessionId, messageId, content);
    }
    
    public static StreamResponse streamEnd(UUID sessionId, UUID messageId) {
        return new StreamResponse("stream_end", sessionId, messageId, "");
    }
    
    public static StreamResponse error(UUID sessionId, String errorMessage) {
        return new StreamResponse("error", sessionId, null, errorMessage);
    }
    
    // Inner class for metadata
    public static class StreamMetadata {
        private LocalDateTime timestamp = LocalDateTime.now();
        private Integer tokenCount;
        private Long processingTime;
        
        // Getters and Setters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        public Integer getTokenCount() {
            return tokenCount;
        }
        
        public void setTokenCount(Integer tokenCount) {
            this.tokenCount = tokenCount;
        }
        
        public Long getProcessingTime() {
            return processingTime;
        }
        
        public void setProcessingTime(Long processingTime) {
            this.processingTime = processingTime;
        }
    }
}