class FileHandler {
    constructor() {
        this.maxFileSize = 5 * 1024 * 1024; // 5MB
        this.allowedTypes = {
            image: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
            audio: ['audio/mp3', 'audio/wav', 'audio/ogg', 'audio/m4a'],
            document: ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
        };
        
        this.init();
    }
    
    init() {
        this.bindFileEvents();
        this.setupDropZone();
    }
    
    bindFileEvents() {
        $(document).on('change', '#fileInput', (e) => this.handleFileSelect(e));
        $(document).on('click', '.file-preview .remove-file', (e) => this.removeFile(e));
        $(document).on('click', '.download-file', (e) => this.downloadFile(e));
        $(document).on('click', '.preview-file', (e) => this.previewFile(e));
    }
    
    setupDropZone() {
        const chatArea = $('#messagesArea');
        if (chatArea.length) {
            chatArea.on('dragover', (e) => {
                e.preventDefault();
                chatArea.addClass('drag-over');
            });
            
            chatArea.on('dragleave', (e) => {
                e.preventDefault();
                chatArea.removeClass('drag-over');
            });
            
            chatArea.on('drop', (e) => {
                e.preventDefault();
                chatArea.removeClass('drag-over');
                
                const files = e.originalEvent.dataTransfer.files;
                if (files.length > 0) {
                    this.handleFiles(files);
                }
            });
        }
    }
    
    handleFileSelect(event) {
        const files = event.target.files;
        this.handleFiles(files);
    }
    
    handleFiles(files) {
        if (!window.secureMessaging?.currentChatWith) {
            this.showFileError('Selecione um usuário para enviar o arquivo');
            return;
        }
        
        Array.from(files).forEach(file => {
            if (this.validateFile(file)) {
                this.processFile(file);
            }
        });
    }
    
    validateFile(file) {
        // Check file size
        if (file.size > this.maxFileSize) {
            this.showFileError(`Arquivo "${file.name}" é muito grande. Máximo: 5MB`);
            return false;
        }
        
        // Check file type
        const isValidType = Object.values(this.allowedTypes)
            .flat()
            .includes(file.type);
        
        if (!isValidType) {
            this.showFileError(`Tipo de arquivo "${file.type}" não suportado`);
            return false;
        }
        
        return true;
    }
    
    processFile(file) {
        const fileId = 'file_' + Date.now();
        
        // Show file preview
        this.showFilePreview(file, fileId);
        
        // Start encryption and upload process
        this.encryptAndUploadFile(file, fileId);
    }
    
    showFilePreview(file, fileId) {
        const fileType = this.getFileType(file.type);
        const previewHtml = `
            <div class="file-preview card mb-2" data-file-id="${fileId}">
                <div class="card-body p-2">
                    <div class="d-flex align-items-center">
                        <div class="file-icon me-2">
                            ${this.getFileIcon(fileType)}
                        </div>
                        <div class="file-info flex-grow-1">
                            <div class="file-name">${file.name}</div>
                            <div class="file-size text-muted small">${this.formatFileSize(file.size)}</div>
                            <div class="file-status text-info small">
                                <i class="fas fa-lock"></i> Criptografando...
                            </div>
                        </div>
                        <div class="file-actions">
                            <div class="upload-progress" style="display: none;">
                                <div class="progress" style="width: 100px; height: 6px;">
                                    <div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 0%"></div>
                                </div>
                            </div>
                            <button class="btn btn-sm btn-outline-danger remove-file">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        $('#filePreviewArea').append(previewHtml);
    }
    
    encryptAndUploadFile(file, fileId) {
        const preview = $(`.file-preview[data-file-id="${fileId}"]`);
        const progressBar = preview.find('.progress-bar');
        const uploadProgress = preview.find('.upload-progress');
        const statusDiv = preview.find('.file-status');
        
        uploadProgress.show();
        statusDiv.html('<i class="fas fa-shield-alt"></i> Criptografando com PGP...');
        
        // Create FormData for upload
        const formData = new FormData();
        formData.append('file', file);
        formData.append('recipient', window.secureMessaging.currentChatWith);
        
        // Upload encrypted file using real API
        $.ajax({
            url: '/api/files/upload',
            method: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            xhr: function() {
                const xhr = new XMLHttpRequest();
                xhr.upload.addEventListener('progress', function(e) {
                    if (e.lengthComputable) {
                        const percentComplete = (e.loaded / e.total) * 100;
                        progressBar.css('width', percentComplete + '%');
                        
                        if (percentComplete < 50) {
                            statusDiv.html('<i class="fas fa-lock"></i> Criptografando...');
                        } else {
                            statusDiv.html('<i class="fas fa-upload"></i> Enviando...');
                        }
                    }
                });
                return xhr;
            },
            success: (response) => {
                this.onFileUploadComplete(fileId, file, response);
            },
            error: (xhr) => {
                this.onFileUploadError(fileId, xhr.responseJSON?.error || 'Erro no upload');
            }
        });
    }
    
    onFileUploadComplete(fileId, file, response) {
        const preview = $(`.file-preview[data-file-id="${fileId}"]`);
        preview.find('.upload-progress').hide();
        preview.find('.file-status').html('<i class="fas fa-check-circle text-success"></i> Enviado com segurança');
        preview.find('.file-actions').html(`
            <span class="text-success">
                <i class="fas fa-shield-alt"></i> Criptografado
            </span>
        `);
        
        // Send file message through secure messaging
        if (window.secureMessaging && response.success) {
            this.sendFileMessage(file, response.fileId);
        }
        
        // Remove preview after delay
        setTimeout(() => {
            preview.remove();
        }, 3000);
    }
    
    sendFileMessage(file, fileId) {
        // Add file message to chat
        const fileType = this.getFileType(file.type);
        const messageHtml = `
            <div class="message-bubble message-sent">
                <div class="card bg-primary text-white">
                    <div class="card-body p-3">
                        <div class="d-flex align-items-center mb-2">
                            <div class="file-icon me-2">
                                ${this.getFileIcon(fileType, 'text-white')}
                            </div>
                            <div class="file-info flex-grow-1">
                                <div class="file-name">${file.name}</div>
                                <div class="file-size text-white-50 small">${this.formatFileSize(file.size)}</div>
                            </div>
                        </div>
                        <div class="file-actions">
                            <button class="btn btn-sm btn-outline-light download-file" data-file-id="${fileId}">
                                <i class="fas fa-download"></i> Download
                            </button>
                            ${fileType === 'image' ? `
                                <button class="btn btn-sm btn-outline-light preview-file ms-1" data-file-id="${fileId}" data-file-type="image">
                                    <i class="fas fa-eye"></i> Preview
                                </button>
                            ` : ''}
                        </div>
                        <small class="text-white-50 d-block mt-2">
                            <i class="fas fa-lock"></i> Arquivo criptografado • ${new Date().toLocaleTimeString()}
                        </small>
                    </div>
                </div>
            </div>
        `;
        
        $('#messagesArea').append(messageHtml);
        window.secureMessaging?.scrollToBottom();
    }
    
    onFileUploadError(fileId, error) {
        const preview = $(`.file-preview[data-file-id="${fileId}"]`);
        preview.find('.upload-progress').hide();
        preview.find('.file-status').html('<i class="fas fa-times-circle text-danger"></i> Erro no envio');
        preview.find('.file-actions').html(`
            <span class="text-danger">
                <i class="fas fa-exclamation-triangle"></i> Falhou
            </span>
        `);
        
        this.showFileError(error);
    }
    
    removeFile(event) {
        const preview = $(event.target).closest('.file-preview');
        preview.remove();
    }
    
    downloadFile(event) {
        const fileId = $(event.target).closest('[data-file-id]').data('file-id');
        if (!fileId) return;
        
        // Show downloading status
        const btn = $(event.target);
        const originalText = btn.html();
        btn.html('<i class="fas fa-spinner fa-spin"></i> Baixando...');
        
        // Create download link
        const link = document.createElement('a');
        link.href = `/api/files/download/${fileId}`;
        link.click();
        
        // Reset button after delay
        setTimeout(() => {
            btn.html(originalText);
        }, 2000);
    }
    
    previewFile(event) {
        const fileId = $(event.target).closest('[data-file-id]').data('file-id');
        const fileType = $(event.target).closest('[data-file-type]').data('file-type');
        
        if (fileType === 'image') {
            this.showImagePreview(fileId);
        } else if (fileType === 'audio') {
            this.showAudioPlayer(fileId);
        } else {
            this.downloadFile(event);
        }
    }
    
    showImagePreview(fileId) {
        const modalHtml = `
            <div class="modal fade" id="imagePreviewModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-image"></i> Preview da Imagem (Descriptografada)
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body text-center">
                            <div class="loading-spinner">
                                <i class="fas fa-spinner fa-spin fa-2x"></i>
                                <p>Descriptografando imagem...</p>
                            </div>
                            <img src="/api/files/preview/${fileId}" class="img-fluid d-none" alt="Preview" 
                                 onload="$('.loading-spinner').hide(); $(this).removeClass('d-none');"
                                 onerror="$('.loading-spinner').html('<i class=&quot;fas fa-exclamation-triangle&quot;></i> Erro ao carregar preview');">
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        $('body').append(modalHtml);
        $('#imagePreviewModal').modal('show');
        
        // Remove modal after closing
        $('#imagePreviewModal').on('hidden.bs.modal', function() {
            $(this).remove();
        });
    }
    
    getFileType(mimeType) {
        if (this.allowedTypes.image.includes(mimeType)) return 'image';
        if (this.allowedTypes.audio.includes(mimeType)) return 'audio';
        if (this.allowedTypes.document.includes(mimeType)) return 'document';
        return 'other';
    }
    
    getFileIcon(fileType, colorClass = '') {
        const icons = {
            image: `<i class="fas fa-image ${colorClass || 'text-primary'}"></i>`,
            audio: `<i class="fas fa-music ${colorClass || 'text-success'}"></i>`,
            document: `<i class="fas fa-file-pdf ${colorClass || 'text-danger'}"></i>`,
            other: `<i class="fas fa-file ${colorClass || 'text-secondary'}"></i>`
        };
        
        return icons[fileType] || icons.other;
    }
    
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    showFileError(message) {
        const errorHtml = `
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="fas fa-exclamation-triangle"></i> ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        $('#fileErrors').html(errorHtml);
        setTimeout(() => $('#fileErrors').empty(), 5000);
    }
}

// Initialize file handler
$(document).ready(() => {
    window.fileHandler = new FileHandler();
    
    // Add enhanced drag-over styles
    $('<style>')
        .prop('type', 'text/css')
        .html(`
            .drag-over::after {
                content: 'Solte o arquivo aqui para criptografar e enviar';
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background: rgba(0, 123, 255, 0.9);
                color: white;
                padding: 10px 20px;
                border-radius: 5px;
                font-weight: bold;
                z-index: 1000;
            }
            
            .file-preview {
                transition: all 0.2s ease;
                border-left: 4px solid #007bff;
            }
            
            .file-preview:hover {
                transform: translateY(-1px);
                box-shadow: 0 0.25rem 0.5rem rgba(0, 0, 0, 0.1);
            }
            
            .file-icon {
                font-size: 1.2em;
            }
            
            .crypto-status.active {
                color: #28a745;
            }
            
            .crypto-status.inactive {
                color: #dc3545;
            }
        `)
        .appendTo('head');
});