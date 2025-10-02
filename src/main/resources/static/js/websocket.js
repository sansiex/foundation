// WebSocket Connection Manager
class WebSocketManager {
    constructor() {
        this.socket = null;
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.messageHandlers = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
    }

    connect() {
        return new Promise((resolve, reject) => {
            try {
                // Use SockJS with STOMP
                this.socket = new SockJS('/ws/chat');
                this.stompClient = Stomp.over(this.socket);
                
                // Disable debug logging in production
                this.stompClient.debug = (msg) => {
                    if (window.DEBUG_MODE) {
                        console.log('STOMP:', msg);
                    }
                };

                this.stompClient.connect({}, 
                    (frame) => {
                        console.log('WebSocket connected:', frame);
                        this.connected = true;
                        this.reconnectAttempts = 0;
                        resolve(frame);
                    },
                    (error) => {
                        console.error('WebSocket connection error:', error);
                        this.connected = false;
                        this.handleReconnect();
                        reject(error);
                    }
                );

                // Handle socket close
                this.socket.onclose = (event) => {
                    console.log('WebSocket closed:', event);
                    this.connected = false;
                    this.handleReconnect();
                };

            } catch (error) {
                console.error('Failed to create WebSocket connection:', error);
                reject(error);
            }
        });
    }

    disconnect() {
        if (this.stompClient && this.connected) {
            this.stompClient.disconnect(() => {
                console.log('WebSocket disconnected');
                this.connected = false;
            });
        }
    }

    subscribeToSession(sessionId, messageHandler) {
        if (!this.connected || !this.stompClient) {
            console.warn('WebSocket not connected, cannot subscribe to session');
            return null;
        }

        const topic = `/topic/session/${sessionId}`;
        
        try {
            const subscription = this.stompClient.subscribe(topic, (message) => {
                try {
                    const data = JSON.parse(message.body);
                    messageHandler(data);
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error);
                }
            });

            this.subscriptions.set(sessionId, subscription);
            this.messageHandlers.set(sessionId, messageHandler);
            
            console.log(`Subscribed to session: ${sessionId}`);
            return subscription;
            
        } catch (error) {
            console.error('Failed to subscribe to session:', error);
            return null;
        }
    }

    unsubscribeFromSession(sessionId) {
        const subscription = this.subscriptions.get(sessionId);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(sessionId);
            this.messageHandlers.delete(sessionId);
            console.log(`Unsubscribed from session: ${sessionId}`);
        }
    }

    sendMessage(destination, message) {
        if (!this.connected || !this.stompClient) {
            console.warn('WebSocket not connected, cannot send message');
            return false;
        }

        try {
            this.stompClient.send(destination, {}, JSON.stringify(message));
            return true;
        } catch (error) {
            console.error('Failed to send WebSocket message:', error);
            return false;
        }
    }

    sendChatMessage(sessionId, content, type = 'text') {
        const message = {
            sessionId: sessionId,
            content: content,
            type: type
        };

        return this.sendMessage('/app/chat.stream', message);
    }

    handleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            window.app.ui.showToast('Connection lost. Please refresh the page.', 'error');
            return;
        }

        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
        
        console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);
        
        setTimeout(() => {
            this.connect()
                .then(() => {
                    console.log('Reconnected successfully');
                    window.app.ui.showToast('Connection restored', 'success');
                    
                    // Re-subscribe to active sessions
                    this.resubscribeToActiveSessions();
                })
                .catch((error) => {
                    console.error('Reconnection failed:', error);
                });
        }, delay);
    }

    resubscribeToActiveSessions() {
        // Re-subscribe to the current active session if any
        const currentSessionId = window.app.chat.getCurrentSessionId();
        if (currentSessionId) {
            const handler = this.messageHandlers.get(currentSessionId);
            if (handler) {
                this.subscribeToSession(currentSessionId, handler);
            }
        }
    }

    isConnected() {
        return this.connected;
    }

    getConnectionStatus() {
        return {
            connected: this.connected,
            reconnectAttempts: this.reconnectAttempts,
            subscriptions: this.subscriptions.size
        };
    }
}

// Server-Sent Events fallback
class SSEManager {
    constructor() {
        this.eventSources = new Map();
    }

    subscribeToSession(sessionId, messageHandler) {
        const url = `/api/chat/stream/${sessionId}`;
        
        try {
            const eventSource = new EventSource(url);
            
            eventSource.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    messageHandler(data);
                } catch (error) {
                    console.error('Error parsing SSE message:', error);
                }
            };

            eventSource.onerror = (error) => {
                console.error('SSE connection error:', error);
                eventSource.close();
                this.eventSources.delete(sessionId);
            };

            this.eventSources.set(sessionId, eventSource);
            console.log(`SSE subscription created for session: ${sessionId}`);
            
            return eventSource;
            
        } catch (error) {
            console.error('Failed to create SSE connection:', error);
            return null;
        }
    }

    unsubscribeFromSession(sessionId) {
        const eventSource = this.eventSources.get(sessionId);
        if (eventSource) {
            eventSource.close();
            this.eventSources.delete(sessionId);
            console.log(`SSE subscription closed for session: ${sessionId}`);
        }
    }

    closeAll() {
        this.eventSources.forEach((eventSource, sessionId) => {
            eventSource.close();
            console.log(`Closed SSE connection for session: ${sessionId}`);
        });
        this.eventSources.clear();
    }
}

// Initialize WebSocket manager
window.wsManager = new WebSocketManager();
window.sseManager = new SSEManager();

// Attempt WebSocket connection on page load
document.addEventListener('DOMContentLoaded', () => {
    // Try WebSocket first, fallback to SSE if needed
    window.wsManager.connect()
        .then(() => {
            console.log('WebSocket connection established');
        })
        .catch((error) => {
            console.warn('WebSocket connection failed, will use HTTP streaming:', error);
        });
});

// Include SockJS and STOMP libraries
function loadWebSocketLibraries() {
    return new Promise((resolve, reject) => {
        const sockjsScript = document.createElement('script');
        sockjsScript.src = 'https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js';
        sockjsScript.onload = () => {
            const stompScript = document.createElement('script');
            stompScript.src = 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js';
            stompScript.onload = () => resolve();
            stompScript.onerror = reject;
            document.head.appendChild(stompScript);
        };
        sockjsScript.onerror = reject;
        document.head.appendChild(sockjsScript);
    });
}

// Load libraries when script loads
loadWebSocketLibraries()
    .then(() => {
        console.log('WebSocket libraries loaded successfully');
    })
    .catch((error) => {
        console.error('Failed to load WebSocket libraries:', error);
    });