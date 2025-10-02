package com.aichat.foundation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;
    
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileAttachment> attachments = new ArrayList<>();
    
    // Constructors
    public Message() {}
    
    public Message(String content, MessageType type, ChatSession session) {
        this.content = content;
        this.type = type;
        this.session = session;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public ChatSession getSession() {
        return session;
    }
    
    public void setSession(ChatSession session) {
        this.session = session;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
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
    
    public List<FileAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<FileAttachment> attachments) {
        this.attachments = attachments;
    }
    
    // Helper methods
    public void addAttachment(FileAttachment attachment) {
        attachments.add(attachment);
        attachment.setMessage(this);
    }
    
    public void removeAttachment(FileAttachment attachment) {
        attachments.remove(attachment);
        attachment.setMessage(null);
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", createdAt=" + createdAt +
                '}';
    }
}