// API Client for REST endpoints
class APIClient {
    constructor() {
        this.baseURL = '/api';
        this.defaultHeaders = {
            'Content-Type': 'application/json',
        };
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const config = {
            headers: { ...this.defaultHeaders, ...options.headers },
            ...options
        };

        try {
            const response = await fetch(url, config);
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
            }

            // Handle different response types
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error(`API request failed: ${endpoint}`, error);
            throw error;
        }
    }

    // Chat Sessions
    async createSession(title) {
        return this.request('/chat/sessions', {
            method: 'POST',
            body: JSON.stringify({ title: title || 'New Chat' })
        });
    }

    async getSessions() {
        return this.request('/chat/sessions');
    }

    async getSession(sessionId) {
        return this.request(`/chat/sessions/${sessionId}`);
    }

    async deleteSession(sessionId) {
        return this.request(`/chat/sessions/${sessionId}`, {
            method: 'DELETE'
        });
    }

    async getSessionMessages(sessionId) {
        return this.request(`/chat/sessions/${sessionId}/messages`);
    }

    // Messages
    async sendTextMessage(sessionId, content) {
        // For streaming responses, we'll use the WebSocket or SSE
        // This method is for non-streaming fallback
        return this.request('/chat/message', {
            method: 'POST',
            body: JSON.stringify({
                sessionId: sessionId,
                content: content,
                type: 'text'
            })
        });
    }

    async sendMultimodalMessage(sessionId, content, file) {
        const formData = new FormData();
        formData.append('sessionId', sessionId);
        formData.append('content', content);
        formData.append('file', file);

        return this.request('/chat/message/multimodal', {
            method: 'POST',
            headers: {}, // Remove Content-Type to let browser set boundary for FormData
            body: formData
        });
    }

    // Files
    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);

        return this.request('/files/upload', {
            method: 'POST',
            headers: {}, // Remove Content-Type for FormData
            body: formData
        });
    }

    async getFile(fileId) {
        const response = await fetch(`${this.baseURL}/files/${fileId}`);
        if (!response.ok) {
            throw new Error(`Failed to fetch file: ${response.statusText}`);
        }
        return response.blob();
    }

    async getFileInfo(fileId) {
        return this.request(`/files/${fileId}/info`);
    }

    async deleteFile(fileId) {
        return this.request(`/files/${fileId}`, {
            method: 'DELETE'
        });
    }

    // Health check
    async healthCheck() {
        return this.request('/chat/health');
    }

    // Streaming message via HTTP (fallback)
    async streamMessage(sessionId, content, onChunk, onComplete, onError) {
        try {
            const response = await fetch(`${this.baseURL}/chat/message`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify({
                    sessionId: sessionId,
                    content: content,
                    type: 'text'
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const { done, value } = await reader.read();
                
                if (done) {
                    onComplete && onComplete();
                    break;
                }

                const chunk = decoder.decode(value, { stream: true });
                const lines = chunk.split('\n');

                for (const line of lines) {
                    if (line.trim()) {
                        try {
                            const data = JSON.parse(line);
                            onChunk && onChunk(data);
                        } catch (parseError) {
                            console.warn('Failed to parse streaming chunk:', parseError);
                        }
                    }
                }
            }
        } catch (error) {
            console.error('Streaming request failed:', error);
            onError && onError(error);
        }
    }
}

// Utility functions
const api = {
    // Response helpers
    handleResponse: async (response) => {
        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: response.statusText }));
            throw new Error(error.message || `HTTP ${response.status}`);
        }
        return response.json();
    },

    // Error helpers
    handleError: (error, context = '') => {
        console.error(`API Error ${context}:`, error);
        
        let message = 'An unexpected error occurred';
        if (error.message) {
            message = error.message;
        } else if (typeof error === 'string') {
            message = error;
        }

        // Show user-friendly error message
        window.app?.ui?.showToast(message, 'error');
        
        return { error: message };
    },

    // Format helpers
    formatFileSize: (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    formatDate: (dateString) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        if (diffDays < 7) return `${diffDays}d ago`;
        
        return date.toLocaleDateString();
    },

    // Validation helpers
    validateFileType: (file, allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']) => {
        return allowedTypes.includes(file.type);
    },

    validateFileSize: (file, maxSizeBytes = 10 * 1024 * 1024) => { // 10MB default
        return file.size <= maxSizeBytes;
    }
};

// Initialize API client
window.apiClient = new APIClient();
window.api = api;