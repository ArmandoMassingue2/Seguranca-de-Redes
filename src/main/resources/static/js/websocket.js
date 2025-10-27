class WebSocketManager {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 5000;
        
        this.connect();
    }
    
    connect() {
        console.log('Connecting to WebSocket...');
        
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        // Disable debug logging
        this.stompClient.debug = null;
        
        this.stompClient.connect({}, 
            (frame) => this.onConnected(frame),
            (error) => this.onError(error)
        );
    }
    
    onConnected(frame) {
        console.log('Connected to WebSocket:', frame);
        this.connected = true;
        this.reconnectAttempts = 0;
        
        // Subscribe to personal message queue
        this.stompClient.subscribe('/user/queue/messages', (message) => {
            this.handleIncomingMessage(JSON.parse(message.body));
        });
        
        // Subscribe to typing indicators
        this.stompClient.subscribe('/user/queue/typing', (message) => {
            this.handleTypingIndicator(message.body);
        });
        
        // Subscribe to key exchange notifications
        this.stompClient.subscribe('/user/queue/keyexchange', (message) => {
            this.handleKeyExchangeNotification(JSON.parse(message.body));
        });
        
        // Notify that user is online
        this.sendUserStatus('online');
        
        this.updateConnectionStatus(true);
    }
    
    onError(error) {
        console.error('WebSocket connection error:', error);
        this.connected = false;
        this.updateConnectionStatus(false);
        
        // Attempt to reconnect
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            setTimeout(() => this.connect(), this.reconnectInterval);
        } else {
            console.error('Max reconnection attempts reached');
            this.showConnectionError();
        }
    }
    
    handleIncomingMessage(messageData) {
        console.log('Received message:', messageData);
        
        // Show notification if not in current chat
        if (messageData.senderUsername !== window.secureMessaging?.currentChatWith) {
            window.secureMessaging?.showNotification(
                `Nova mensagem de ${messageData.senderUsername}`,
                'Mensagem criptografada recebida'
            );
            
            // Update unread count
            this.updateUnreadCount(messageData.senderUsername);
        }
        
        // Add to chat if current conversation
        if (window.secureMessaging?.currentChatWith === messageData.senderUsername) {
            window.secureMessaging?.addMessageToChat(messageData);
        }
    }
    
    handleTypingIndicator(message) {
        console.log('Typing indicator:', message);
        $('#typingIndicator').text(message).show();
        
        // Hide after 3 seconds
        setTimeout(() => {
            $('#typingIndicator').hide();
        }, 3000);
    }
    
    handleKeyExchangeNotification(data) {
        console.log('Key exchange notification:', data);
        
        if (data.type === 'request') {
            // Show key exchange request modal
            this.showKeyExchangeRequest(data.from);
        } else if (data.type === 'completed') {
            // Show completion notification
            window.secureMessaging?.showNotification(
                'Troca de Chaves Concluída',
                `Chaves trocadas com segurança com ${data.from}`
            );
        }
    }
    
    sendMessage(messageData) {
        if (this.connected && this.stompClient) {
            this.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(messageData));
        } else {
            console.error('WebSocket not connected');
        }
    }
    
    sendTypingIndicator(recipient) {
        if (this.connected && this.stompClient) {
            this.stompClient.send('/app/typing', {}, recipient);
        }
    }
    
    sendUserStatus(status) {
        if (this.connected && this.stompClient) {
            this.stompClient.send('/app/user.status', {}, JSON.stringify({
                username: window.currentUser,
                status: status
            }));
        }
    }
    
    updateUnreadCount(senderUsername) {
        const userItem = $(`.user-item[data-username="${senderUsername}"]`);
        const badge = userItem.find('.unread-count');
        
        let count = parseInt(badge.text()) || 0;
        count++;
        
        badge.text(count).show();
    }
    
    updateConnectionStatus(connected) {
        const statusIndicator = $('#connectionStatus');
        if (connected) {
            statusIndicator.removeClass('text-danger').addClass('text-success')
                           .html('<i class="fas fa-circle"></i> Conectado');
        } else {
            statusIndicator.removeClass('text-success').addClass('text-danger')
                           .html('<i class="fas fa-circle"></i> Desconectado');
        }
    }
    
    showConnectionError() {
        const errorHtml = `
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="fas fa-exclamation-triangle"></i> 
                Conexão perdida. Algumas funcionalidades podem não estar disponíveis.
                <button type="button" class="btn btn-sm btn-outline-danger ms-2" onclick="window.wsManager.connect()">
                    Reconectar
                </button>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        $('#connectionAlerts').html(errorHtml);
    }
    
    showKeyExchangeRequest(fromUser) {
        const modalHtml = `
            <div class="modal fade" id="keyExchangeRequestModal" tabindex="-1">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-key"></i> Solicitação de Troca de Chaves
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <p><strong>${fromUser}</strong> está solicitando uma troca de chaves Diffie-Hellman.</p>
                            <p class="text-muted">Isso estabelecerá uma comunicação mais segura entre vocês.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Recusar</button>
                            <button type="button" class="btn btn-primary" onclick="window.wsManager.acceptKeyExchange('${fromUser}')">
                                Aceitar
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        $('body').append(modalHtml);
        $('#keyExchangeRequestModal').modal('show');
    }
    
    acceptKeyExchange(fromUser) {
        // Implementation for accepting key exchange
        console.log('Accepting key exchange from:', fromUser);
        $('#keyExchangeRequestModal').modal('hide');
    }
    
    disconnect() {
        if (this.stompClient) {
            this.sendUserStatus('offline');
            this.stompClient.disconnect();
        }
        this.connected = false;
        console.log('Disconnected from WebSocket');
    }
}

// Initialize WebSocket when page loads
$(document).ready(() => {
    window.wsManager = new WebSocketManager();
    
    // Disconnect when page unloads
    $(window).on('beforeunload', () => {
        window.wsManager?.disconnect();
    });
});
