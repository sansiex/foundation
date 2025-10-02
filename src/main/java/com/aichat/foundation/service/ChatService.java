package com.aichat.foundation.service;

import com.aichat.foundation.dto.*;
import com.aichat.foundation.entity.*;
import com.aichat.foundation.exception.ChatServiceException;
import com.aichat.foundation.repository.ChatSessionRepository;
import com.aichat.foundation.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final ModelService modelService;
    private final FileService fileService;
    
    public ChatService(ChatSessionRepository chatSessionRepository,
                      MessageRepository messageRepository,
                      ModelService modelService,
                      FileService fileService) {
        this.chatSessionRepository = chatSessionRepository;
        this.messageRepository = messageRepository;
        this.modelService = modelService;
        this.fileService = fileService;
    }
    
    /**
     * Create a new chat session
     */
    public ChatSessionDto createSession(String title, String userId) {
        if (title == null || title.trim().isEmpty()) {
            title = "New Chat - " + LocalDateTime.now().toString();
        }
        
        ChatSession session = new ChatSession(title.trim(), userId);
        ChatSession savedSession = chatSessionRepository.save(session);
        
        return convertToSessionDto(savedSession);
    }
    
    /**
     * Get all sessions for a user
     */
    @Transactional(readOnly = true)
    public List<ChatSessionDto> getUserSessions(String userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return sessions.stream()
            .map(this::convertToSessionDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a specific session with messages
     */
    @Transactional(readOnly = true)
    public Optional<ChatSessionDto> getSessionWithMessages(UUID sessionId, String userId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findByIdAndUserId(sessionId, userId);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ChatSession session = sessionOpt.get();
        ChatSessionDto sessionDto = convertToSessionDto(session);
        
        // Get messages for this session
        List<Message> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        return Optional.of(sessionDto);
    }
    
    /**
     * Delete a chat session
     */
    public boolean deleteSession(UUID sessionId, String userId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findByIdAndUserId(sessionId, userId);
        
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        chatSessionRepository.delete(sessionOpt.get());
        return true;
    }
    
    /**
     * Process a text message and return streaming response
     */
    public Flux<StreamResponse> processTextMessage(ChatMessageRequest request, String userId) {
        // Validate session ownership
        if (!chatSessionRepository.existsByIdAndUserId(request.getSessionId(), userId)) {
            return Flux.error(new ChatServiceException("Session not found or access denied"));
        }
        
        // Save user message
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new ChatServiceException("Session not found"));
        
        Message userMessage = new Message(request.getContent(), MessageType.USER, session);
        messageRepository.save(userMessage);
        
        // Create assistant message placeholder
        Message assistantMessage = new Message("", MessageType.ASSISTANT, session);
        Message savedAssistantMessage = messageRepository.save(assistantMessage);
        
        // Build conversation context
        String conversationContext = buildConversationContext(request.getSessionId());
        String enhancedPrompt = modelService.createEnhancedPrompt(request.getContent(), conversationContext);
        
        // Process with model service and collect response
        StringBuilder responseBuilder = new StringBuilder();
        
        return modelService.processTextRequest(request.getSessionId(), savedAssistantMessage.getId(), enhancedPrompt)
            .doOnNext(streamResponse -> {
                if ("stream_chunk".equals(streamResponse.getType())) {
                    responseBuilder.append(streamResponse.getContent());
                }
            })
            .doOnComplete(() -> {
                // Save complete response to database
                savedAssistantMessage.setContent(responseBuilder.toString());
                messageRepository.save(savedAssistantMessage);
                
                // Update session timestamp
                session.setUpdatedAt(LocalDateTime.now());
                chatSessionRepository.save(session);
            })
            .onErrorMap(throwable -> new ChatServiceException("Failed to process message", throwable));
    }
    
    /**
     * Process a multimodal message (text + image) and return streaming response
     */
    public Flux<StreamResponse> processMultimodalMessage(UUID sessionId, String content, 
                                                       MultipartFile file, String userId) {
        // Validate session ownership
        if (!chatSessionRepository.existsByIdAndUserId(sessionId, userId)) {
            return Flux.error(new ChatServiceException("Session not found or access denied"));
        }
        
        try {
            // Save user message with attachment
            ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatServiceException("Session not found"));
            
            Message userMessage = new Message(content, MessageType.USER, session);
            Message savedUserMessage = messageRepository.save(userMessage);
            
            // Upload and attach file
            FileAttachmentDto fileAttachment = fileService.uploadFile(file, savedUserMessage);
            
            // Get file content for model processing
            byte[] imageData = fileService.getFileContent(fileAttachment.getId());
            
            // Create assistant message placeholder
            Message assistantMessage = new Message("", MessageType.ASSISTANT, session);
            Message savedAssistantMessage = messageRepository.save(assistantMessage);
            
            // Build conversation context
            String conversationContext = buildConversationContext(sessionId);
            String enhancedPrompt = modelService.createMultimodalPrompt(content, "User uploaded an image");
            
            // Process with model service
            StringBuilder responseBuilder = new StringBuilder();
            
            return modelService.processMultimodalRequest(sessionId, savedAssistantMessage.getId(), 
                                                       enhancedPrompt, imageData)
                .doOnNext(streamResponse -> {
                    if ("stream_chunk".equals(streamResponse.getType())) {
                        responseBuilder.append(streamResponse.getContent());
                    }
                })
                .doOnComplete(() -> {
                    // Save complete response to database
                    savedAssistantMessage.setContent(responseBuilder.toString());
                    messageRepository.save(savedAssistantMessage);
                    
                    // Update session timestamp
                    session.setUpdatedAt(LocalDateTime.now());
                    chatSessionRepository.save(session);
                })
                .onErrorMap(throwable -> new ChatServiceException("Failed to process multimodal message", throwable));
                
        } catch (Exception e) {
            return Flux.error(new ChatServiceException("Failed to process multimodal message", e));
        }
    }
    
    /**
     * Get messages for a session
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getSessionMessages(UUID sessionId, String userId) {
        // Validate session ownership
        if (!chatSessionRepository.existsByIdAndUserId(sessionId, userId)) {
            throw new ChatServiceException("Session not found or access denied");
        }
        
        List<Message> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        return messages.stream()
            .map(this::convertToMessageDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Build conversation context from recent messages
     */
    private String buildConversationContext(UUID sessionId) {
        List<Message> recentMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        // Limit to last 10 messages for context
        int startIndex = Math.max(0, recentMessages.size() - 10);
        List<Message> contextMessages = recentMessages.subList(startIndex, recentMessages.size());
        
        StringBuilder context = new StringBuilder();
        for (Message message : contextMessages) {
            String sender = message.getType() == MessageType.USER ? "User" : "Assistant";
            context.append(sender).append(": ").append(message.getContent()).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Convert ChatSession entity to DTO
     */
    private ChatSessionDto convertToSessionDto(ChatSession session) {
        int messageCount = (int) messageRepository.countBySessionId(session.getId());
        
        return new ChatSessionDto(
            session.getId(),
            session.getTitle(),
            session.getUserId(),
            session.getStatus().name(),
            session.getCreatedAt(),
            session.getUpdatedAt(),
            messageCount
        );
    }
    
    /**
     * Convert Message entity to DTO
     */
    private MessageDto convertToMessageDto(Message message) {
        List<FileAttachmentDto> attachments = fileService.getFileAttachmentsByMessageId(message.getId());
        
        return new MessageDto(
            message.getId(),
            message.getSession().getId(),
            message.getContent(),
            message.getType().name(),
            message.getCreatedAt(),
            message.getMetadata(),
            attachments
        );
    }
}