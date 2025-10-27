class SecureMessaging {
    constructor() {
        this.currentUser = null;
        this.stompClient = null;
        this.currentChatWith = null;
        this.keyExchangeSessions = new Map();
        
        this.init();
    }
    
    init() {
        this.bindEvents();
        this.loadCurrentUser();
        this.setupNotifications();
    }
    
    bindEvents() {
        // Message sending
        $(document).on('click', '#sendMessageBtn', () => this.sendMessage());
        $(document).on('keypress', '#messageInput', (e) => {
            if (e.which === 13) this.sendMessage();
        });
        
        // File handling
        $(document).on('click', '#attachFileBtn', () => $('#fileInput').click());
        $(document).on('change', '#fileInput', (e) => this.handleFileUpload(e));
        
        // User selection
        $(document).on('click', '.user-item', (e) => {
            const username = $(e.currentTarget).data('username');
            this.selectUser(username);
        });
        
        // Key exchange
        $(document).on('click', '#initiateKeyExchangeBtn', () => this.initiateKeyExchange());
        
        // Certificate verification
        $(document).on('click', '.verify-cert-btn', (e) => {
            const certId = $(e.currentTarget).data('cert-id');
            this.verifyCertificate(certId);
        });
    }
    
    loadCurrentUser() {
        // Extract current user from page context
        this.currentUser = window.currentUser || $('meta[name="current-user"]').attr('content');
    }
    
    setupNotifications() {
        // Request notification permission
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }
    
    sendMessage() {
        const messageText = $('#messageInput').val().trim();
        if (!messageText || !this.currentChatWith) return;
        
        // Show sending indicator
        this.showMessageStatus('Criptografando e enviando...', 'info');
        
        $.ajax({
            url: '/api/messages/send',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                recipient: this.currentChatWith,
                content: messageText,
                messageType: 'TEXT'
            }),
            success: (response) => {
                $('#messageInput').val('');
                this.showMessageStatus('Mensagem enviada com segurança', 'success');
                
                // Add message to UI immediately
                this.addMessageToChat({
                    senderUsername: this.currentUser,
                    content: messageText,
                    timestamp: new Date(),
                    encrypted: true
                });
            },
            error: (xhr) => {
                this.showMessageStatus('Erro: ' + xhr.responseJSON?.error, 'danger');
            }
        });
    }
    
    selectUser(username) {
        this.currentChatWith = username;
        $('.user-item').removeClass('active');
        $(`.user-item[data-username="${username}"]`).addClass('active');
        
        $('#currentChatUser').text(username);
        $('#messageInput, #sendMessageBtn, #attachFileBtn').prop('disabled', false);
        
        this.loadConversation(username);
    }
    
    loadConversation(username) {
        $('#messagesArea').html('<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Carregando conversa segura...</div>');
        
        $.get(`/api/messages/conversation/${username}`)
            .done((messages) => {
                $('#messagesArea').empty();
                messages.forEach((message) => {
                    this.decryptAndDisplayMessage(message);
                });
            })
            .fail(() => {
                $('#messagesArea').html('<div class="text-center text-danger">Erro ao carregar conversa</div>');
            });
    }
    
    decryptAndDisplayMessage(message) {
        $.get(`/api/messages/decrypt/${message.id}`)
            .done((response) => {
                message.content = response.content;
                message.decrypted = true;
                this.addMessageToChat(message);
            })
            .fail(() => {
                message.content = '[Erro na descriptografia]';
                message.decrypted = false;
                this.addMessageToChat(message);
            });
    }
    
    addMessageToChat(message) {
        const isSent = message.senderUsername === this.currentUser;
        const timestamp = new Date(message.timestamp).toLocaleTimeString();
        const encryptionIcon = message.decrypted !== false ? 
            '<i class="fas fa-lock text-success" title="Descriptografado com segurança"></i>' :
            '<i class="fas fa-exclamation-triangle text-warning" title="Erro na descriptografia"></i>';
        
        const messageHtml = `
            <div class="message-bubble ${isSent ? 'message-sent' : 'message-received'}">
                <div class="card ${isSent ? 'bg-primary text-white' : 'bg-light'}">
                    <div class="card-body p-3">
                        <p class="mb-2">${this.escapeHtml(message.content)}</p>
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="${isSent ? 'text-white-50' : 'text-muted'}">
                                ${encryptionIcon} ${timestamp}
                            </small>
                            ${message.messageType !== 'TEXT' ? `<span class="badge bg-secondary">${message.messageType}</span>` : ''}
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        $('#messagesArea').append(messageHtml);
        this.scrollToBottom();
    }
    
    handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;
        
        // File size limit (5MB)
        if (file.size > 5 * 1024 * 1024) {
            alert('Arquivo muito grande. Limite: 5MB');
            return;
        }
        
        // TODO: Implement encrypted file upload
        this.showMessageStatus('Upload de arquivos será implementado em breve', 'info');
    }
    
    initiateKeyExchange() {
        const targetUser = $('#keyExchangeUser').val();
        if (!targetUser) {
            alert('Selecione um usuário');
            return;
        }
        
        this.showKeyExchangeProgress(0, 'Iniciando troca de chaves Diffie-Hellman...');
        
        $.ajax({
            url: '/api/keyexchange/initiate',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ recipient: targetUser }),
            success: (response) => {
                if (response.success) {
                    this.completeKeyExchange(response.sessionId, targetUser);
                }
            },
            error: (xhr) => {
                alert('Erro na troca de chaves: ' + xhr.responseJSON?.error);
                this.hideKeyExchangeProgress();
            }
        });
    }
    
    completeKeyExchange(sessionId, targetUser) {
        this.showKeyExchangeProgress(50, 'Trocando chaves públicas...');
        
        setTimeout(() => {
            $.post(`/api/keyexchange/complete/${sessionId}`)
                .done((response) => {
                    if (response.success) {
                        this.showKeyExchangeProgress(100, 'Troca de chaves concluída com sucesso!');
                        setTimeout(() => {
                            $('#keyExchangeModal').modal('hide');
                            this.hideKeyExchangeProgress();
                        }, 2000);
                    }
                })
                .fail((xhr) => {
                    alert('Erro ao completar troca de chaves: ' + xhr.responseJSON?.error);
                    this.hideKeyExchangeProgress();
                });
        }, 1500);
    }
    
    verifyCertificate(certId) {
        const btn = $(`.verify-cert-btn[data-cert-id="${certId}"]`);
        btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Verificando...');
        
        $.post(`/pki/api/certificates/verify/${certId}`)
            .done((response) => {
                this.showCertificateResult(response);
                btn.prop('disabled', false).html('<i class="fas fa-check-circle"></i> Verificar');
            })
            .fail(() => {
                this.showCertificateResult({ valid: false, error: 'Erro na verificação' });
                btn.prop('disabled', false).html('<i class="fas fa-check-circle"></i> Verificar');
            });
    }
    
    showMessageStatus(message, type) {
        const alertClass = `alert-${type}`;
        const statusHtml = `<div class="alert ${alertClass} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;
        
        $('#messageStatus').html(statusHtml);
        setTimeout(() => $('#messageStatus').empty(), 3000);
    }
    
    showKeyExchangeProgress(percentage, status) {
        $('#keyExchangeProgress').show();
        $('.progress-bar').css('width', percentage + '%');
        $('#keyExchangeStatus').text(status);
        
        if (percentage === 100) {
            $('.progress-bar').removeClass('progress-bar-striped progress-bar-animated');
        }
    }
    
    hideKeyExchangeProgress() {
        $('#keyExchangeProgress').hide();
        $('.progress-bar').css('width', '0%').addClass('progress-bar-striped progress-bar-animated');
        $('#initiateKeyExchangeBtn').prop('disabled', false);
    }
    
    showCertificateResult(response) {
        const resultDiv = $('#verificationResult');
        const alertClass = response.valid ? 'alert-success' : 'alert-danger';
        const icon = response.valid ? 'fa-check-circle' : 'fa-times-circle';
        const message = response.valid ? 'Certificado válido e confiável' : 'Certificado inválido';
        
        resultDiv.html(`
            <div class="alert ${alertClass}">
                <i class="fas ${icon}"></i> ${message}
                ${response.fingerprint ? `<br><small class="certificate-fingerprint">Fingerprint: ${response.fingerprint}</small>` : ''}
                ${response.error ? `<br><small>Erro: ${response.error}</small>` : ''}
            </div>
        `).show();
    }
    
    scrollToBottom() {
        const messagesArea = $('#messagesArea');
        messagesArea.scrollTop(messagesArea[0].scrollHeight);
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    showNotification(title, message) {
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(title, {
                body: message,
                icon: '/img/logo.png'
            });
        }
    }
}

// Initialize when DOM is ready
$(document).ready(() => {
    window.secureMessaging = new SecureMessaging();
});