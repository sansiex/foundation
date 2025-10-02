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
            // Check if SockJS and Stomp are available
            if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
                console.warn('SockJS or STOMP not available, WebSocket connection skipped');
                reject(new Error('WebSocket libraries not available'));
                return;
            }
            
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
                        console.log('WebSocket connection failed, HTTP streaming will be used:', error);
                        this.connected = false;
                        // Don't attempt reconnection on initial connection failure
                        // Let the application use HTTP streaming instead
                        reject(error);
                    }
                );

                // Handle socket close
                this.socket.onclose = (event) => {
                    console.log('WebSocket closed, falling back to HTTP streaming');
                    this.connected = false;
                    // Only attempt reconnection if we were previously connected
                    if (this.reconnectAttempts === 0) {
                        this.handleReconnect();
                    }
                };

            } catch (error) {
                console.warn('Failed to create WebSocket connection:', error);
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
            console.log('Max WebSocket reconnection attempts reached, using HTTP streaming fallback');
            // Don't show error message to user, just fall back to HTTP streaming silently
            return;
        }

        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
        
        console.log(`Attempting WebSocket reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);
        
        setTimeout(() => {
            this.connect()
                .then(() => {
                    console.log('WebSocket reconnected successfully');
                    // Only show success message if it was previously failed
                    if (this.reconnectAttempts > 1) {
                        window.app?.ui?.showToast('WebSocket connection restored', 'success');
                    }
                    
                    // Re-subscribe to active sessions
                    this.resubscribeToActiveSessions();
                })
                .catch((error) => {
                    console.warn('WebSocket reconnection failed, continuing with HTTP fallback:', error);
                    // Don't show error to user, HTTP streaming still works
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
    // Load WebSocket libraries first, then attempt connection
    loadWebSocketLibraries()
        .then(() => {
            console.log('WebSocket libraries loaded successfully');
            // Try WebSocket connection (optional enhancement)
            return window.wsManager.connect();
        })
        .then(() => {
            console.log('WebSocket connection established (optional enhancement)');
        })
        .catch((error) => {
            console.log('WebSocket not available, using HTTP streaming (this is normal):', error.message);
            // This is expected and normal - HTTP streaming will be used instead
        });
});

// Include SockJS and STOMP libraries from local files
function loadWebSocketLibraries() {
    return new Promise((resolve, reject) => {
        const sockjsScript = document.createElement('script');
        sockjsScript.src = '/lib/sockjs.min.js';
        sockjsScript.onload = () => {
            const stompScript = document.createElement('script');
            stompScript.src = '/lib/stomp.umd.min.js';
            stompScript.onload = () => resolve();
            stompScript.onerror = (error) => {
                console.warn('Failed to load STOMP library, WebSocket features will be limited:', error);
                resolve(); // Continue without STOMP
            };
            document.head.appendChild(stompScript);
        };
        sockjsScript.onerror = (error) => {
            console.warn('Failed to load SockJS library, WebSocket features will be limited:', error);
            resolve(); // Continue without SockJS
        };
        document.head.appendChild(sockjsScript);
    });
}

// Load libraries when script loads
loadWebSocketLibraries()
    .then(() => {
        console.log('WebSocket libraries loaded, ready for optional connection');
    })
    .catch((error) => {
        console.log('WebSocket libraries not available, HTTP streaming will be used:', error);
    });