package com.aichat.foundation.controller;

import com.aichat.foundation.dto.ChatSessionDto;
import com.aichat.foundation.dto.MessageDto;
import com.aichat.foundation.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createSession_ShouldReturnCreatedSession() throws Exception {
        // Given
        ChatSessionDto sessionDto = new ChatSessionDto(
            UUID.randomUUID(),
            "Test Session",
            "default-user",
            "ACTIVE",
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        );
        
        when(chatService.createSession(eq("Test Session"), eq("default-user")))
            .thenReturn(sessionDto);

        // When & Then
        mockMvc.perform(post("/api/chat/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Session\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Session"))
                .andExpect(jsonPath("$.userId").value("default-user"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUserSessions_ShouldReturnSessionsList() throws Exception {
        // Given
        List<ChatSessionDto> sessions = Arrays.asList(
            new ChatSessionDto(UUID.randomUUID(), "Session 1", "default-user", "ACTIVE", 
                              LocalDateTime.now(), LocalDateTime.now(), 5),
            new ChatSessionDto(UUID.randomUUID(), "Session 2", "default-user", "ACTIVE", 
                              LocalDateTime.now(), LocalDateTime.now(), 3)
        );
        
        when(chatService.getUserSessions("default-user")).thenReturn(sessions);

        // When & Then
        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Session 1"))
                .andExpect(jsonPath("$[0].messageCount").value(5))
                .andExpect(jsonPath("$[1].title").value("Session 2"))
                .andExpect(jsonPath("$[1].messageCount").value(3));
    }

    @Test
    void getSession_ShouldReturnSession_WhenExists() throws Exception {
        // Given
        UUID sessionId = UUID.randomUUID();
        ChatSessionDto sessionDto = new ChatSessionDto(
            sessionId,
            "Test Session",
            "default-user",
            "ACTIVE",
            LocalDateTime.now(),
            LocalDateTime.now(),
            2
        );
        
        when(chatService.getSessionWithMessages(sessionId, "default-user"))
            .thenReturn(Optional.of(sessionDto));

        // When & Then
        mockMvc.perform(get("/api/chat/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.title").value("Test Session"));
    }

    @Test
    void getSession_ShouldReturn404_WhenNotExists() throws Exception {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(chatService.getSessionWithMessages(sessionId, "default-user"))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/chat/sessions/{id}", sessionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSession_ShouldReturn200_WhenDeleted() throws Exception {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(chatService.deleteSession(sessionId, "default-user")).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/chat/sessions/{id}", sessionId))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSession_ShouldReturn404_WhenNotFound() throws Exception {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(chatService.deleteSession(sessionId, "default-user")).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/chat/sessions/{id}", sessionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSessionMessages_ShouldReturnMessagesList() throws Exception {
        // Given
        UUID sessionId = UUID.randomUUID();
        List<MessageDto> messages = Arrays.asList(
            new MessageDto(UUID.randomUUID(), sessionId, "Hello", "USER", 
                          LocalDateTime.now(), null, Arrays.asList()),
            new MessageDto(UUID.randomUUID(), sessionId, "Hi there!", "ASSISTANT", 
                          LocalDateTime.now(), null, Arrays.asList())
        );
        
        when(chatService.getSessionMessages(sessionId, "default-user")).thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/chat/sessions/{id}/messages", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Hello"))
                .andExpect(jsonPath("$[0].type").value("USER"))
                .andExpect(jsonPath("$[1].content").value("Hi there!"))
                .andExpect(jsonPath("$[1].type").value("ASSISTANT"));
    }

    @Test
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Chat service is running"));
    }
}