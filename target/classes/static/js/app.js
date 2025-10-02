// Main Application Controller
class Application {
    constructor() {
        this.initialized = false;
        this.ui = null;
        this.chat = null;
        this.fileUpload = null;
    }

    async initialize() {
        if (this.initialized) {
            return;
        }

        try {
            console.log('Initializing AI Chat Foundation...');

            // Initialize components
            this.ui = window.uiManager;
            this.chat = window.chatManager;
            this.fileUpload = window.fileUploadManager;

            // Perform health check
            await this.healthCheck();

            // Initialize chat system
            await this.chat.initialize();

            // Setup global error handling
            this.setupErrorHandling();

            // Setup periodic health checks
            this.setupHealthChecks();

            this.initialized = true;
            console.log('Application initialized successfully');

            // Show welcome message
            this.ui.showToast('Welcome to AI Chat Foundation', 'success');

        } catch (error) {
            console.error('Failed to initialize application:', error);
            this.ui?.showToast('Failed to initialize application', 'error');
        }
    }

    async healthCheck() {
        try {
            await window.apiClient.healthCheck();
            console.log('Health check passed');
        } catch (error) {
            console.warn('Health check failed:', error);
            this.ui?.showToast('Service may be unavailable', 'warning');
        }
    }

    setupErrorHandling() {
        // Global error handler
        window.addEventListener('error', (event) => {
            console.error('Global error:', event.error);
            this.ui?.showToast('An unexpected error occurred', 'error');
        });

        // Unhandled promise rejection handler
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
            this.ui?.showToast('An unexpected error occurred', 'error');
            event.preventDefault();
        });

        // Network error detection
        window.addEventListener('offline', () => {
            this.ui?.showToast('Connection lost. Some features may not work.', 'error');
        });

        window.addEventListener('online', () => {
            this.ui?.showToast('Connection restored', 'success');
            this.healthCheck();
        });
    }

    setupHealthChecks() {
        // Periodic health check every 5 minutes
        setInterval(async () => {
            try {
                await this.healthCheck();
            } catch (error) {
                console.warn('Periodic health check failed:', error);
            }
        }, 5 * 60 * 1000);

        // WebSocket connection monitoring
        setInterval(() => {
            if (window.wsManager && !window.wsManager.isConnected()) {
                console.log('WebSocket disconnected, attempting reconnection...');
                window.wsManager.connect().catch(error => {
                    console.warn('WebSocket reconnection failed:', error);
                });
            }
        }, 30 * 1000);
    }

    // Public API methods
    async createNewChat() {
        return await this.chat.createNewSession();
    }

    async loadChat(sessionId) {
        return await this.chat.loadSession(sessionId);
    }

    async sendMessage(content, file = null) {
        if (file) {
            this.fileUpload.selectFile(file);
        }
        
        if (content) {
            this.ui.elements.messageInput.value = content;
        }
        
        return await this.chat.sendMessage();
    }

    getConnectionStatus() {
        return {
            websocket: window.wsManager?.getConnectionStatus() || { connected: false },
            online: navigator.onLine,
            initialized: this.initialized
        };
    }

    // Development helpers
    debug() {
        return {
            app: this,
            ui: this.ui,
            chat: this.chat,
            fileUpload: this.fileUpload,
            wsManager: window.wsManager,
            apiClient: window.apiClient,
            status: this.getConnectionStatus()
        };
    }
}

// Initialize application when DOM is loaded
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Create global app instance
        window.app = new Application();
        
        // Initialize application
        await window.app.initialize();
        
        // Expose debug interface in development
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            window.DEBUG_MODE = true;
            window.debug = () => window.app.debug();
            console.log('Debug mode enabled. Use debug() for debugging information.');
        }
        
    } catch (error) {
        console.error('Failed to start application:', error);
        
        // Show basic error message if UI is not available
        const errorDiv = document.createElement('div');
        errorDiv.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: #f8d7da;
            color: #721c24;
            padding: 1rem 2rem;
            border-radius: 6px;
            border: 1px solid #f5c6cb;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            z-index: 10000;
        `;
        errorDiv.textContent = 'Failed to initialize application. Please refresh the page.';
        document.body.appendChild(errorDiv);
    }
});

// Export for potential module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = Application;
}