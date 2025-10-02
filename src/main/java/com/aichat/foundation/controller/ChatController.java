package com.aichat.foundation.controller;

import com.aichat.foundation.dto.*;
import com.aichat.foundation.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Create a new chat session
     */
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDto> createSession(@RequestBody CreateSessionRequest request) {
        // For demo purposes, using a default user ID
        String userId = "default-user";
        
        ChatSessionDto session = chatService.createSession(request.getTitle(), userId);
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get all sessions for the user
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDto>> getUserSessions() {
        String userId = "default-user";
        
        List<ChatSessionDto> sessions = chatService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Get a specific session with messages
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<ChatSessionDto> getSession(@PathVariable UUID id) {
        String userId = "default-user";
        
        Optional<ChatSessionDto> session = chatService.getSessionWithMessages(id, userId);
        return session.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Delete a chat session
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        String userId = "default-user";
        
        boolean deleted = chatService.deleteSession(id, userId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * Get messages for a session
     */
    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<MessageDto>> getSessionMessages(@PathVariable UUID id) {
        String userId = "default-user";
        
        try {
            List<MessageDto> messages = chatService.getSessionMessages(id, userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Send a text message
     */
    @PostMapping("/message")
    public Flux<StreamResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        String userId = "default-user";
        
        return chatService.processTextMessage(request, userId);
    }
    
    /**
     * Send a multimodal message (text + image)
     */
    @PostMapping(value = "/message/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<StreamResponse> sendMultimodalMessage(
            @RequestParam("sessionId") UUID sessionId,
            @RequestParam("content") String content,
            @RequestParam("file") MultipartFile file) {
        
        String userId = "default-user";
        
        return chatService.processMultimodalMessage(sessionId, content, file, userId);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("Chat service is running", "UP"));
    }
    
    // Inner classes for request/response DTOs
    public static class CreateSessionRequest {
        private String title;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }
    
    public static class HealthResponse {
        private String message;
        private String status;
        
        public HealthResponse(String message, String status) {
            this.message = message;
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
}