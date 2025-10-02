package com.aichat.foundation.service;

import com.aichat.foundation.client.OllamaClient;
import com.aichat.foundation.dto.StreamResponse;
import com.aichat.foundation.exception.ModelServiceException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class ModelService {
    
    private final OllamaClient ollamaClient;
    
    public ModelService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }
    
    /**
     * Process text-only request and return streaming response
     */
    public Flux<StreamResponse> processTextRequest(UUID sessionId, UUID messageId, String prompt) {
        return Flux.concat(
            // Send stream start
            Flux.just(StreamResponse.streamStart(sessionId, messageId)),
            
            // Process streaming response from OLLAMA
            ollamaClient.sendTextMessage(prompt)
                .map(content -> StreamResponse.streamChunk(sessionId, messageId, content))
                .onErrorMap(throwable -> new ModelServiceException("Failed to process text request", throwable)),
            
            // Send stream end
            Flux.just(StreamResponse.streamEnd(sessionId, messageId))
        );
    }
    
    /**
     * Process multimodal request (text + image) and return streaming response
     */
    public Flux<StreamResponse> processMultimodalRequest(UUID sessionId, UUID messageId, String prompt, byte[] imageData) {
        return Flux.concat(
            // Send stream start
            Flux.just(StreamResponse.streamStart(sessionId, messageId)),
            
            // Process streaming response from OLLAMA
            ollamaClient.sendMultimodalMessage(prompt, imageData)
                .map(content -> StreamResponse.streamChunk(sessionId, messageId, content))
                .onErrorMap(throwable -> new ModelServiceException("Failed to process multimodal request", throwable)),
            
            // Send stream end
            Flux.just(StreamResponse.streamEnd(sessionId, messageId))
        );
    }
    
    /**
     * Test OLLAMA service availability
     */
    public boolean isServiceAvailable() {
        return ollamaClient.isHealthy();
    }
    
    /**
     * Create enhanced prompt with context
     */
    public String createEnhancedPrompt(String userMessage, String conversationContext) {
        StringBuilder promptBuilder = new StringBuilder();
        
        if (conversationContext != null && !conversationContext.isEmpty()) {
            promptBuilder.append("Previous conversation context:\n");
            promptBuilder.append(conversationContext);
            promptBuilder.append("\n\n");
        }
        
        promptBuilder.append("User: ").append(userMessage);
        promptBuilder.append("\n\nAssistant: ");
        
        return promptBuilder.toString();
    }
    
    /**
     * Create multimodal prompt with image description
     */
    public String createMultimodalPrompt(String userMessage, String imageDescription) {
        StringBuilder promptBuilder = new StringBuilder();
        
        if (imageDescription != null && !imageDescription.isEmpty()) {
            promptBuilder.append("Image description: ").append(imageDescription).append("\n\n");
        }
        
        promptBuilder.append("User message: ").append(userMessage);
        promptBuilder.append("\n\nPlease analyze the image and respond to the user's message. Assistant: ");
        
        return promptBuilder.toString();
    }
}