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
     * Create enhanced prompt with context and language matching
     */
    public String createEnhancedPrompt(String userMessage, String conversationContext) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add language instruction based on user message language
        String languageInstruction = detectAndCreateLanguageInstruction(userMessage);
        promptBuilder.append(languageInstruction).append("\n\n");
        
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
     * Create multimodal prompt with image description and language matching
     */
    public String createMultimodalPrompt(String userMessage, String imageDescription) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add language instruction based on user message language
        String languageInstruction = detectAndCreateLanguageInstruction(userMessage);
        promptBuilder.append(languageInstruction).append("\n\n");
        
        if (imageDescription != null && !imageDescription.isEmpty()) {
            promptBuilder.append("Image description: ").append(imageDescription).append("\n\n");
        }
        
        promptBuilder.append("User message: ").append(userMessage);
        promptBuilder.append("\n\nPlease analyze the image and respond to the user's message. Assistant: ");
        
        return promptBuilder.toString();
    }
    
    /**
     * Detect the language of user message and create appropriate instruction
     */
    private String detectAndCreateLanguageInstruction(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Please respond in the same language as the user's question.";
        }
        
        // Simple language detection based on character patterns
        boolean hasChinese = userMessage.matches(".*[\u4e00-\u9fff]+.*");
        boolean hasJapanese = userMessage.matches(".*[\u3040-\u309f\u30a0-\u30ff]+.*");
        boolean hasKorean = userMessage.matches(".*[\uac00-\ud7af]+.*");
        boolean hasRussian = userMessage.matches(".*[\u0400-\u04ff]+.*");
        boolean hasArabic = userMessage.matches(".*[\u0600-\u06ff]+.*");
        
        if (hasChinese) {
            return "请使用中文回答用户的问题。务必保持与用户问题相同的语言。";
        } else if (hasJapanese) {
            return "ユーザーの質問に日本語で答えてください。ユーザーの質問と同じ言語を保ってください。";
        } else if (hasKorean) {
            return "사용자의 질문에 한국어로 답변해 주세요. 사용자 질문과 같은 언어를 유지해 주세요.";
        } else if (hasRussian) {
            return "Пожалуйста, отвечайте на вопросы пользователя на русском языке. Сохраняйте тот же язык, что и в вопросе пользователя.";
        } else if (hasArabic) {
            return "يرجى الإجابة على أسئلة المستخدم باللغة العربية. حافظ على نفس لغة سؤال المستخدم.";
        } else {
            // Default to English for Latin script or mixed content
            return "Please respond in English. Keep your response clear and helpful.";
        }
    }
}