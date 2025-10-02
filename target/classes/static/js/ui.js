// UI Management and Utilities
class UIManager {
    constructor() {
        this.activeSession = null;
        this.sidebarVisible = true;
        this.elements = {};
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.elements = {
            // Main containers
            sidebar: document.getElementById('sidebar'),
            chatContainer: document.getElementById('chat-container'),
            welcomeScreen: document.getElementById('welcome-screen'),
            messagesContainer: document.getElementById('messages-container'),
            
            // Session elements
            sessionsList: document.getElementById('sessions-list'),
            chatTitle: document.getElementById('chat-title'),
            
            // Input elements
            messageInput: document.getElementById('message-input'),
            fileInput: document.getElementById('file-input'),
            sendBtn: document.getElementById('send-btn'),
            
            // Buttons
            newChatBtn: document.getElementById('new-chat-btn'),
            startChatBtn: document.getElementById('start-chat-btn'),
            deleteChatBtn: document.getElementById('delete-chat-btn'),
            sidebarToggle: document.getElementById('sidebar-toggle'),
            refreshSessions: document.getElementById('refresh-sessions'),
            
            // File upload
            fileUploadZone: document.getElementById('file-upload-zone'),
            filePreview: document.getElementById('file-preview'),
            removeFileBtn: document.getElementById('remove-file'),
            
            // Utility
            loadingOverlay: document.getElementById('loading-overlay'),
            toastContainer: document.getElementById('toast-container')
        };
    }

    bindEvents() {
        // Auto-resize textarea
        if (this.elements.messageInput) {
            this.elements.messageInput.addEventListener('input', this.autoResizeTextarea.bind(this));
            this.elements.messageInput.addEventListener('keydown', this.handleKeydown.bind(this));
        }

        // Button events
        if (this.elements.newChatBtn) {
            this.elements.newChatBtn.addEventListener('click', () => window.app.chat.createNewSession());
        }
        
        if (this.elements.startChatBtn) {
            this.elements.startChatBtn.addEventListener('click', () => window.app.chat.createNewSession());
        }
        
        if (this.elements.sendBtn) {
            this.elements.sendBtn.addEventListener('click', () => window.app.chat.sendMessage());
        }
        
        if (this.elements.deleteChatBtn) {
            this.elements.deleteChatBtn.addEventListener('click', () => window.app.chat.deleteCurrentSession());
        }
        
        if (this.elements.sidebarToggle) {
            this.elements.sidebarToggle.addEventListener('click', this.toggleSidebar.bind(this));
        }
        
        if (this.elements.refreshSessions) {
            this.elements.refreshSessions.addEventListener('click', () => window.app.chat.loadSessions());
        }

        // File upload events
        if (this.elements.fileInput) {
            this.elements.fileInput.addEventListener('change', this.handleFileSelect.bind(this));
        }
        
        if (this.elements.removeFileBtn) {
            this.elements.removeFileBtn.addEventListener('click', this.removeSelectedFile.bind(this));
        }

        // Window events
        window.addEventListener('resize', this.handleResize.bind(this));
        
        // Prevent form submission on Enter if not intended
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && e.target === this.elements.messageInput && !e.shiftKey) {
                e.preventDefault();
                window.app.chat.sendMessage();
            }
        });
    }

    autoResizeTextarea() {
        const textarea = this.elements.messageInput;
        if (textarea) {
            textarea.style.height = 'auto';
            textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
            
            // Enable/disable send button based on content
            const hasContent = textarea.value.trim().length > 0;
            this.elements.sendBtn.disabled = !hasContent;
        }
    }

    handleKeydown(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            window.app.chat.sendMessage();
        }
    }

    handleFileSelect(e) {
        const file = e.target.files[0];
        if (file) {
            window.app.fileUpload.selectFile(file);
        }
    }

    removeSelectedFile() {
        window.app.fileUpload.clearSelection();
    }

    toggleSidebar() {
        this.sidebarVisible = !this.sidebarVisible;
        if (this.elements.sidebar) {
            this.elements.sidebar.classList.toggle('hidden', !this.sidebarVisible);
        }
    }

    handleResize() {
        // Handle responsive behavior
        if (window.innerWidth <= 768) {
            this.sidebarVisible = false;
            if (this.elements.sidebar) {
                this.elements.sidebar.classList.add('hidden');
            }
        }
    }

    showWelcomeScreen() {
        if (this.elements.welcomeScreen) {
            this.elements.welcomeScreen.classList.remove('hidden');
        }
        if (this.elements.chatContainer) {
            this.elements.chatContainer.classList.add('hidden');
        }
    }

    showChatContainer() {
        if (this.elements.welcomeScreen) {
            this.elements.welcomeScreen.classList.add('hidden');
        }
        if (this.elements.chatContainer) {
            this.elements.chatContainer.classList.remove('hidden');
        }
    }

    updateChatTitle(title) {
        if (this.elements.chatTitle) {
            this.elements.chatTitle.textContent = title;
        }
    }

    clearMessages() {
        if (this.elements.messagesContainer) {
            this.elements.messagesContainer.innerHTML = '';
        }
    }

    scrollToBottom() {
        if (this.elements.messagesContainer) {
            this.elements.messagesContainer.scrollTop = this.elements.messagesContainer.scrollHeight;
        }
    }

    showLoading(message = 'Processing...') {
        if (this.elements.loadingOverlay) {
            this.elements.loadingOverlay.classList.remove('hidden');
            const text = this.elements.loadingOverlay.querySelector('p');
            if (text) text.textContent = message;
        }
    }

    hideLoading() {
        if (this.elements.loadingOverlay) {
            this.elements.loadingOverlay.classList.add('hidden');
        }
    }

    showToast(message, type = 'info', duration = 5000) {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        
        if (this.elements.toastContainer) {
            this.elements.toastContainer.appendChild(toast);
            
            // Auto-remove toast
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, duration);
        }
    }

    clearInput() {
        if (this.elements.messageInput) {
            this.elements.messageInput.value = '';
            this.autoResizeTextarea();
        }
    }

    focusInput() {
        if (this.elements.messageInput) {
            this.elements.messageInput.focus();
        }
    }

    disableInput(disabled = true) {
        if (this.elements.messageInput) {
            this.elements.messageInput.disabled = disabled;
        }
        if (this.elements.sendBtn) {
            this.elements.sendBtn.disabled = disabled;
        }
        if (this.elements.fileInput) {
            this.elements.fileInput.disabled = disabled;
        }
    }

    setActiveSession(sessionId) {
        this.activeSession = sessionId;
        
        // Update UI to reflect active session
        const sessionItems = document.querySelectorAll('.session-item');
        sessionItems.forEach(item => {
            const isActive = item.dataset.sessionId === sessionId;
            item.classList.toggle('active', isActive);
        });
    }

    // Utility methods
    createElement(tag, className = '', content = '') {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (content) element.textContent = content;
        return element;
    }

    formatTimestamp(date) {
        const now = new Date();
        const messageDate = new Date(date);
        const diff = now - messageDate;
        
        if (diff < 60000) return 'Just now';
        if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
        if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
        
        return messageDate.toLocaleDateString();
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    truncateText(text, maxLength = 50) {
        if (text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    renderMarkdown(text) {
        try {
            // Configure marked with safe options
            if (typeof marked !== 'undefined') {
                const renderer = new marked.Renderer();
                
                // Customize code block rendering
                renderer.code = function(code, language) {
                    const validLang = language && language.match(/^[a-zA-Z0-9_+-]*$/) ? language : '';
                    const escapedCode = code.replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    return `<pre><code class="language-${validLang}">${escapedCode}</code></pre>`;
                };
                
                // Customize inline code rendering  
                renderer.codespan = function(code) {
                    const escapedCode = code.replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    return `<code class="inline-code">${escapedCode}</code>`;
                };
                
                // Customize link rendering to be safe
                renderer.link = function(href, title, text) {
                    const safeHref = href && href.match(/^https?:\/\//) ? href : '#';
                    const titleAttr = title ? ` title="${title.replace(/"/g, '&quot;')}"` : '';
                    return `<a href="${safeHref}" target="_blank" rel="noopener noreferrer"${titleAttr}>${text}</a>`;
                };
                
                // Customize table rendering with responsive wrapper
                renderer.table = function(header, body) {
                    if (body) body = `<tbody>${body}</tbody>`;
                    return `<div class="table-container"><table>
<thead>
${header}</thead>
${body}</table></div>
`;
                };
                
                // Configure marked options
                marked.setOptions({
                    renderer: renderer,
                    highlight: null,
                    breaks: true,
                    gfm: true,          // GitHub Flavored Markdown (includes tables)
                    tables: true,       // Explicitly enable tables
                    headerIds: false,
                    mangle: false
                });
                
                // Parse markdown
                let html = marked.parse(text);
                
                // Sanitize with DOMPurify if available
                if (typeof DOMPurify !== 'undefined') {
                    html = DOMPurify.sanitize(html, {
                        ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 'code', 'pre', 'blockquote', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'a', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'div'],
                        ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'title', 'align'],
                        ADD_ATTR: ['target', 'rel'],
                        ALLOW_DATA_ATTR: false
                    });
                }
                
                return html;
            }
        } catch (error) {
            console.warn('Markdown rendering failed, falling back to plain text:', error);
        }
        
        // Fallback to escaped HTML if markdown fails
        return this.escapeHtml(text);
    }
}

// Initialize UI manager
window.uiManager = new UIManager();