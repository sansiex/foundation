package com.aichat.foundation.service;

import com.aichat.foundation.dto.ChatMessageRequest;
import com.aichat.foundation.dto.ChatSessionDto;
import com.aichat.foundation.entity.ChatSession;
import com.aichat.foundation.entity.Message;
import com.aichat.foundation.entity.MessageType;
import com.aichat.foundation.entity.SessionStatus;
import com.aichat.foundation.repository.ChatSessionRepository;
import com.aichat.foundation.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ModelService modelService;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ChatService chatService;

    private ChatSession testSession;
    private String testUserId;
    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user";
        testSessionId = UUID.randomUUID();
        
        testSession = new ChatSession("Test Chat", testUserId);
        testSession.setId(testSessionId);
        testSession.setStatus(SessionStatus.ACTIVE);
        testSession.setCreatedAt(LocalDateTime.now());
        testSession.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createSession_ShouldCreateNewSession() {
        // Given
        String title = "New Chat Session";
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // When
        ChatSessionDto result = chatService.createSession(title, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testSession.getId(), result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(testUserId, result.getUserId());
        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    void createSession_WithNullTitle_ShouldCreateSessionWithDefaultTitle() {
        // Given
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // When
        ChatSessionDto result = chatService.createSession(null, testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.getTitle().startsWith("New Chat - "));
        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    void getUserSessions_ShouldReturnUserSessions() {
        // Given
        List<ChatSession> sessions = Arrays.asList(testSession);
        when(chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(testUserId)).thenReturn(sessions);
        when(messageRepository.countBySessionId(testSessionId)).thenReturn(5L);

        // When
        List<ChatSessionDto> result = chatService.getUserSessions(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSession.getId(), result.get(0).getId());
        assertEquals(5, result.get(0).getMessageCount());
        verify(chatSessionRepository).findByUserIdOrderByUpdatedAtDesc(testUserId);
    }

    @Test
    void getSessionWithMessages_ShouldReturnSession_WhenSessionExists() {
        // Given
        when(chatSessionRepository.findByIdAndUserId(testSessionId, testUserId))
            .thenReturn(Optional.of(testSession));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(testSessionId))
            .thenReturn(Arrays.asList());
        when(messageRepository.countBySessionId(testSessionId)).thenReturn(0L);

        // When
        Optional<ChatSessionDto> result = chatService.getSessionWithMessages(testSessionId, testUserId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testSession.getId(), result.get().getId());
        verify(chatSessionRepository).findByIdAndUserId(testSessionId, testUserId);
    }

    @Test
    void getSessionWithMessages_ShouldReturnEmpty_WhenSessionNotFound() {
        // Given
        when(chatSessionRepository.findByIdAndUserId(testSessionId, testUserId))
            .thenReturn(Optional.empty());

        // When
        Optional<ChatSessionDto> result = chatService.getSessionWithMessages(testSessionId, testUserId);

        // Then
        assertFalse(result.isPresent());
        verify(chatSessionRepository).findByIdAndUserId(testSessionId, testUserId);
        verify(messageRepository, never()).findBySessionIdOrderByCreatedAtAsc(any());
    }

    @Test
    void deleteSession_ShouldDeleteSession_WhenSessionExists() {
        // Given
        when(chatSessionRepository.findByIdAndUserId(testSessionId, testUserId))
            .thenReturn(Optional.of(testSession));

        // When
        boolean result = chatService.deleteSession(testSessionId, testUserId);

        // Then
        assertTrue(result);
        verify(chatSessionRepository).delete(testSession);
    }

    @Test
    void deleteSession_ShouldReturnFalse_WhenSessionNotFound() {
        // Given
        when(chatSessionRepository.findByIdAndUserId(testSessionId, testUserId))
            .thenReturn(Optional.empty());

        // When
        boolean result = chatService.deleteSession(testSessionId, testUserId);

        // Then
        assertFalse(result);
        verify(chatSessionRepository, never()).delete(any());
    }

    @Test
    void getSessionMessages_ShouldReturnMessages_WhenSessionExists() {
        // Given
        Message userMessage = new Message("Hello", MessageType.USER, testSession);
        userMessage.setId(UUID.randomUUID());
        userMessage.setCreatedAt(LocalDateTime.now());
        
        Message assistantMessage = new Message("Hi there!", MessageType.ASSISTANT, testSession);
        assistantMessage.setId(UUID.randomUUID());
        assistantMessage.setCreatedAt(LocalDateTime.now());
        
        List<Message> messages = Arrays.asList(userMessage, assistantMessage);
        
        when(chatSessionRepository.existsByIdAndUserId(testSessionId, testUserId)).thenReturn(true);
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(testSessionId)).thenReturn(messages);
        when(fileService.getFileAttachmentsByMessageId(any())).thenReturn(Arrays.asList());

        // When
        var result = chatService.getSessionMessages(testSessionId, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Hello", result.get(0).getContent());
        assertEquals("Hi there!", result.get(1).getContent());
        verify(messageRepository).findBySessionIdOrderByCreatedAtAsc(testSessionId);
    }

    @Test
    void processTextMessage_ShouldThrowException_WhenSessionNotFound() {
        // Given
        ChatMessageRequest request = new ChatMessageRequest();
        request.setSessionId(testSessionId);
        request.setContent("Hello");
        
        when(chatSessionRepository.existsByIdAndUserId(testSessionId, testUserId)).thenReturn(false);

        // When & Then
        assertThrows(Exception.class, () -> {
            chatService.processTextMessage(request, testUserId).blockLast();
        });
        
        verify(chatSessionRepository).existsByIdAndUserId(testSessionId, testUserId);
        verify(messageRepository, never()).save(any());
    }
}