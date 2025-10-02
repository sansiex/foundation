// Chat Management System
class ChatManager {
    constructor() {
        this.currentSessionId = null;
        this.sessions = new Map();
        this.messages = new Map();
        this.streamingMessageId = null;
        this.streamingContent = '';
    }

    async initialize() {
        try {
            await this.loadSessions();
            this.setupEventHandlers();
            console.log('Chat manager initialized');
        } catch (error) {
            console.error('Failed to initialize chat manager:', error);
            window.app.ui.showToast('Failed to load chat sessions', 'error');
        }
    }

    setupEventHandlers() {
        // WebSocket message handler
        this.messageHandler = this.handleStreamingMessage.bind(this);
    }

    async loadSessions() {
        try {
            window.app.ui.showLoading('Loading chat sessions...');
            const sessions = await window.apiClient.getSessions();
            
            this.sessions.clear();
            sessions.forEach(session => {
                this.sessions.set(session.id, session);
            });
            
            this.renderSessionsList();
            
            if (sessions.length === 0) {
                window.app.ui.showWelcomeScreen();
            }
            
        } catch (error) {
            console.error('Failed to load sessions:', error);
            window.app.ui.showToast('Failed to load chat sessions', 'error');
        } finally {
            window.app.ui.hideLoading();
        }
    }

    renderSessionsList() {
        const sessionsList = window.app.ui.elements.sessionsList;
        if (!sessionsList) return;

        if (this.sessions.size === 0) {
            sessionsList.innerHTML = '<div class="loading">No chat sessions yet</div>';
            return;
        }

        const sessionsArray = Array.from(this.sessions.values());
        sessionsArray.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));

        sessionsList.innerHTML = sessionsArray.map(session => `
            <div class="session-item" data-session-id="${session.id}">
                <div class="session-info">
                    <div class="session-title">${window.app.ui.escapeHtml(session.title)}</div>
                    <div class="session-meta">
                        <span>${session.messageCount} messages</span>
                        <span>${window.app.ui.formatTimestamp(session.updatedAt)}</span>
                    </div>
                </div>
                <div class="session-actions">
                    <button class="btn btn-icon btn-small delete-session" data-session-id="${session.id}" title="Delete">
                        <span class="icon">🗑️</span>
                    </button>
                </div>
            </div>
        `).join('');

        // Bind session events
        sessionsList.querySelectorAll('.session-item').forEach(item => {
            item.addEventListener('click', (e) => {
                if (!e.target.closest('.delete-session')) {
                    const sessionId = item.dataset.sessionId;
                    this.loadSession(sessionId);
                }
            });
        });

        sessionsList.querySelectorAll('.delete-session').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const sessionId = btn.dataset.sessionId;
                this.deleteSession(sessionId);
            });
        });
    }

    async createNewSession(title = null) {
        try {
            window.app.ui.showLoading('Creating new chat session...');
            
            const session = await window.apiClient.createSession(title);
            this.sessions.set(session.id, session);
            
            await this.loadSession(session.id);
            this.renderSessionsList();
            
            window.app.ui.showToast('New chat session created', 'success');
            
        } catch (error) {
            console.error('Failed to create session:', error);
            window.app.ui.showToast('Failed to create new chat session', 'error');
        } finally {
            window.app.ui.hideLoading();
        }
    }

    async loadSession(sessionId) {
        try {
            if (this.currentSessionId) {
                // Unsubscribe from previous session
                window.wsManager.unsubscribeFromSession(this.currentSessionId);
            }

            window.app.ui.showLoading('Loading chat session...');
            
            const session = this.sessions.get(sessionId);
            if (!session) {
                throw new Error('Session not found');
            }

            this.currentSessionId = sessionId;
            
            // Load messages
            const messages = await window.apiClient.getSessionMessages(sessionId);
            this.messages.set(sessionId, messages);
            
            // Update UI
            window.app.ui.showChatContainer();
            window.app.ui.updateChatTitle(session.title);
            window.app.ui.setActiveSession(sessionId);
            
            // Render messages
            this.renderMessages(messages);
            
            // Subscribe to WebSocket updates
            if (window.wsManager.isConnected()) {
                window.wsManager.subscribeToSession(sessionId, this.messageHandler);
            }
            
            // Focus input
            window.app.ui.focusInput();
            
        } catch (error) {
            console.error('Failed to load session:', error);
            window.app.ui.showToast('Failed to load chat session', 'error');
        } finally {
            window.app.ui.hideLoading();
        }
    }

    renderMessages(messages) {
        const container = window.app.ui.elements.messagesContainer;
        if (!container) return;

        container.innerHTML = messages.map(message => this.createMessageElement(message)).join('');
        window.app.ui.scrollToBottom();
    }

    createMessageElement(message) {
        const isUser = message.type === 'USER';
        const timestamp = window.app.ui.formatTimestamp(message.createdAt);
        
        let attachmentsHtml = '';
        if (message.attachments && message.attachments.length > 0) {
            attachmentsHtml = `
                <div class="message-attachments">
                    ${message.attachments.map(att => {
                        // For temporary attachments (just uploaded), show preview from blob URL
                        if (att.id && att.id.startsWith('temp-')) {
                            const file = window.app.fileUpload.getSelectedFile();
                            if (file && file.type.startsWith('image/')) {
                                const imageUrl = URL.createObjectURL(file);
                                return `
                                    <div class="attachment-preview">
                                        <img src="${imageUrl}" alt="${att.fileName}" loading="lazy" onload="setTimeout(() => URL.revokeObjectURL('${imageUrl}'), 60000)">
                                        <div class="attachment-info">
                                            <span class="file-name">${att.fileName}</span>
                                            <span class="file-size">${window.app.ui.formatFileSize(att.fileSize)}</span>
                                        </div>
                                    </div>
                                `;
                            }
                        } else {
                            // For saved attachments, use API endpoint
                            return `
                                <div class="attachment-preview">
                                    <img src="/api/files/${att.id}" alt="${att.fileName}" loading="lazy">
                                    <div class="attachment-info">
                                        <span class="file-name">${att.fileName}</span>
                                        <span class="file-size">${window.app.ui.formatFileSize(att.fileSize)}</span>
                                    </div>
                                </div>
                            `;
                        }
                    }).join('')}
                </div>
            `;
        }

        return `
            <div class="message ${isUser ? 'user' : 'assistant'}" data-message-id="${message.id}">
                <div class="message-avatar">
                    ${isUser ? 'U' : 'AI'}
                </div>
                <div class="message-content">
                    <div class="message-bubble">
                        <div class="message-text">${isUser ? window.app.ui.escapeHtml(message.content) : window.app.ui.renderMarkdown(message.content)}</div>
                        ${attachmentsHtml}
                    </div>
                    <div class="message-meta">${timestamp}</div>
                </div>
            </div>
        `;
    }

    async sendMessage() {
        const input = window.app.ui.elements.messageInput;
        const content = input?.value?.trim();
        
        if (!content) {
            window.app.ui.showToast('Please enter a message', 'warning');
            return;
        }

        if (!this.currentSessionId) {
            // Create new session first
            await this.createNewSession();
        }

        try {
            // Check if file is selected
            const selectedFile = window.app.fileUpload.getSelectedFile();
            
            // Create attachment info for UI if file is selected
            let attachments = [];
            if (selectedFile) {
                attachments = [{
                    id: `temp-${Date.now()}`,
                    fileName: selectedFile.name,
                    fileSize: selectedFile.size,
                    fileType: selectedFile.type
                }];
            }
            
            // Add user message to UI immediately with attachments
            this.addUserMessageToUI(content, attachments);
            window.app.ui.clearInput();
            window.app.ui.disableInput(true);
            
            if (selectedFile) {
                await this.sendMultimodalMessage(content, selectedFile);
            } else {
                await this.sendTextMessage(content);
            }

        } catch (error) {
            console.error('Failed to send message:', error);
            window.app.ui.showToast('Failed to send message', 'error');
            // Re-enable input on error
            window.app.ui.disableInput(false);
        }
    }

    async sendTextMessage(content) {
        // Use HTTP streaming since WebSocket might not be available
        this.prepareForStreaming();
        
        try {
            console.log('Sending message to:', this.currentSessionId, 'Content:', content);
            
            const response = await fetch('/api/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    sessionId: this.currentSessionId,
                    content: content,
                    type: 'text'
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // Parse the response as JSON array
            const streamData = await response.json();
            console.log('Received stream data:', streamData);
            
            // Process stream chunks with simulated streaming delay
            await this.processStreamChunksWithDelay(streamData);
            
        } catch (error) {
            console.error('Failed to send message:', error);
            this.handleStreamError(error);
        }
    }

    async sendMultimodalMessage(content, file) {
        try {
            this.prepareForStreaming();
            
            console.log('Sending multimodal message to:', this.currentSessionId, 'Content:', content, 'File:', file.name);
            
            // Create FormData for multipart request
            const formData = new FormData();
            formData.append('sessionId', this.currentSessionId);
            formData.append('content', content);
            formData.append('file', file);
            
            const response = await fetch('/api/chat/message/multimodal', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // Parse the response as JSON array (same as text messages)
            const streamData = await response.json();
            console.log('Received multimodal stream data:', streamData);
            
            // Process stream chunks with simulated streaming delay
            await this.processStreamChunksWithDelay(streamData);
            
            // Clear file selection after successful send
            window.app.fileUpload.clearSelection();
            
        } catch (error) {
            console.error('Failed to send multimodal message:', error);
            this.handleStreamError(error);
            window.app.ui.showToast('Failed to send message with file', 'error');
        }
    }

    async processStreamChunksWithDelay(streamData) {
        // Filter and sort chunks to get proper streaming order
        const startChunk = streamData.find(chunk => chunk.type === 'stream_start');
        const contentChunks = streamData.filter(chunk => chunk.type === 'stream_chunk');
        const endChunk = streamData.find(chunk => chunk.type === 'stream_end');
        
        // Process start chunk immediately
        if (startChunk) {
            console.log('Processing start chunk:', startChunk);
            this.handleStreamingMessage(startChunk);
        }
        
        // Process content chunks with delays to simulate streaming
        for (let i = 0; i < contentChunks.length; i++) {
            const chunk = contentChunks[i];
            console.log('Processing content chunk:', chunk);
            
            // Add the chunk content to our streaming buffer
            this.handleStreamingMessage(chunk);
            
            // Add a small delay between chunks for streaming effect
            // Adjust delay based on content length - shorter for small chunks
            const chunkLength = chunk.content ? chunk.content.length : 1;
            const baseDelay = 30; // Base delay in ms
            const delayPerChar = 2; // Additional delay per character
            const delay = Math.min(baseDelay + (chunkLength * delayPerChar), 150); // Max 150ms delay
            
            await new Promise(resolve => setTimeout(resolve, delay));
        }
        
        // Process end chunk
        if (endChunk) {
            console.log('Processing end chunk:', endChunk);
            this.handleStreamingMessage(endChunk);
        }
    }

    addUserMessageToUI(content, attachments = []) {
        const userMessage = {
            id: `temp-${Date.now()}`,
            content: content,
            type: 'USER',
            createdAt: new Date().toISOString(),
            attachments: attachments
        };

        const container = window.app.ui.elements.messagesContainer;
        if (container) {
            container.insertAdjacentHTML('beforeend', this.createMessageElement(userMessage));
            window.app.ui.scrollToBottom();
        }
    }

    prepareForStreaming() {
        // Add placeholder for assistant response
        this.streamingMessageId = `streaming-${Date.now()}`;
        this.streamingContent = '';
        
        const placeholderMessage = `
            <div class="message assistant streaming" data-message-id="${this.streamingMessageId}">
                <div class="message-avatar">AI</div>
                <div class="message-content">
                    <div class="message-bubble">
                        <p class="message-text"></p>
                    </div>
                    <div class="message-meta">Thinking...</div>
                </div>
            </div>
        `;
        
        const container = window.app.ui.elements.messagesContainer;
        if (container) {
            container.insertAdjacentHTML('beforeend', placeholderMessage);
            window.app.ui.scrollToBottom();
        }
    }

    handleStreamingMessage(data) {
        if (!data || !data.type) {
            console.warn('Invalid streaming message data:', data);
            return;
        }
        
        switch (data.type) {
            case 'stream_start':
                this.handleStreamStart(data);
                break;
            case 'stream_chunk':
                this.handleStreamChunk(data);
                break;
            case 'stream_end':
                this.handleStreamComplete(data);
                break;
            case 'error':
                this.handleStreamError(data);
                break;
            default:
                console.warn('Unknown stream message type:', data.type);
        }
    }

    handleStreamStart(data) {
        console.log('Stream started for message:', data.messageId);
    }

    handleStreamChunk(data) {
        if (data && data.content) {
            this.streamingContent += data.content;
            this.updateStreamingMessage();
        }
    }

    handleStreamComplete(data) {
        console.log('Stream completed');
        this.finalizeStreamingMessage();
        
        // Re-enable input
        window.app.ui.disableInput(false);
        window.app.ui.focusInput();
    }

    handleStreamError(error) {
        console.error('Streaming error:', error);
        this.finalizeStreamingMessage('Error processing your request. Please try again.');
        
        window.app.ui.disableInput(false);
        window.app.ui.showToast('Error processing your message', 'error');
    }

    updateStreamingMessage() {
        const messageElement = document.querySelector(`[data-message-id="${this.streamingMessageId}"]`);
        if (messageElement) {
            const textElement = messageElement.querySelector('.message-text');
            if (textElement) {
                // For streaming, show markdown-rendered content with typing cursor
                const renderedContent = window.app.ui.renderMarkdown(this.streamingContent);
                textElement.innerHTML = renderedContent + '<span class="typing-cursor">|</span>';
            }
            
            // Update meta to show "typing..." 
            const metaElement = messageElement.querySelector('.message-meta');
            if (metaElement) {
                metaElement.textContent = 'Typing...';
            }
            
            window.app.ui.scrollToBottom();
        }
    }

    finalizeStreamingMessage(finalContent = null) {
        const messageElement = document.querySelector(`[data-message-id="${this.streamingMessageId}"]`);
        if (messageElement) {
            messageElement.classList.remove('streaming');
            
            const textElement = messageElement.querySelector('.message-text');
            if (textElement) {
                // Remove typing cursor and show final markdown-rendered content
                const content = finalContent || this.streamingContent;
                textElement.innerHTML = window.app.ui.renderMarkdown(content);
            }
            
            const metaElement = messageElement.querySelector('.message-meta');
            if (metaElement) {
                metaElement.textContent = 'Just now';
            }
        }
        
        this.streamingMessageId = null;
        this.streamingContent = '';
        
        // Re-enable input after streaming is complete
        window.app.ui.disableInput(false);
        window.app.ui.focusInput();
    }

    async deleteSession(sessionId) {
        if (!confirm('Are you sure you want to delete this chat session?')) {
            return;
        }

        try {
            await window.apiClient.deleteSession(sessionId);
            this.sessions.delete(sessionId);
            
            if (this.currentSessionId === sessionId) {
                this.currentSessionId = null;
                window.app.ui.showWelcomeScreen();
            }
            
            this.renderSessionsList();
            window.app.ui.showToast('Chat session deleted', 'success');
            
        } catch (error) {
            console.error('Failed to delete session:', error);
            window.app.ui.showToast('Failed to delete chat session', 'error');
        }
    }

    async deleteCurrentSession() {
        if (this.currentSessionId) {
            await this.deleteSession(this.currentSessionId);
        }
    }

    getCurrentSessionId() {
        return this.currentSessionId;
    }

    getCurrentSession() {
        return this.currentSessionId ? this.sessions.get(this.currentSessionId) : null;
    }
}

// Initialize chat manager
window.chatManager = new ChatManager();