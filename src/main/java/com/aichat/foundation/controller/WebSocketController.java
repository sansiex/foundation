package com.aichat.foundation.controller;

import com.aichat.foundation.dto.StreamResponse;
import com.aichat.foundation.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Controller
public class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    
    public WebSocketController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }
    
    /**
     * Handle streaming chat messages via WebSocket
     */
    @MessageMapping("/chat.stream")
    public void handleStreamingMessage(@Payload StreamMessage message) {
        String userId = "default-user"; // In real implementation, get from security context
        
        // Create streaming response
        Flux<StreamResponse> responseStream = chatService.processTextMessage(
            message.toChatMessageRequest(), userId);
        
        // Send streaming responses to the client
        responseStream.subscribe(
            response -> {
                // Send each chunk to the specific session topic
                messagingTemplate.convertAndSend(
                    "/topic/session/" + message.getSessionId(),
                    response
                );
            },
            error -> {
                // Send error response
                StreamResponse errorResponse = StreamResponse.error(
                    message.getSessionId(), 
                    "Error processing message: " + error.getMessage()
                );
                messagingTemplate.convertAndSend(
                    "/topic/session/" + message.getSessionId(),
                    errorResponse
                );
            }
        );
    }
    
    // Inner class for WebSocket message format
    public static class StreamMessage {
        private UUID sessionId;
        private String content;
        private String type;
        
        public StreamMessage() {}
        
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
        
        public com.aichat.foundation.dto.ChatMessageRequest toChatMessageRequest() {
            com.aichat.foundation.dto.ChatMessageRequest request = 
                new com.aichat.foundation.dto.ChatMessageRequest();
            request.setSessionId(this.sessionId);
            request.setContent(this.content);
            request.setType(this.type != null ? this.type : "text");
            return request;
        }
    }
}