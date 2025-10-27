class CertificateGenerator {
    constructor() {
        this.certificates = new Map();
        this.caKeyPair = null;
        this.caCertificate = null;
        
        this.init();
    }
    
    init() {
        this.bindEvents();
        this.loadCACertificate();
    }
    
    bindEvents() {
        // Geração de certificados
        $(document).on('click', '#generateSelfSignedBtn', () => this.generateSelfSigned());
        $(document).on('click', '#generateCASignedBtn', () => this.generateCASigned());
        $(document).on('click', '#generateCACertBtn', () => this.generateCACertificate());
        
        // Visualização
        $(document).on('click', '.view-cert-btn', (e) => this.viewCertificate(e));
        $(document).on('click', '.export-cert-btn', (e) => this.exportCertificate(e));
        $(document).on('click', '.revoke-cert-btn', (e) => this.revokeCertificate(e));
        
        // Verificação
        $(document).on('click', '#verifyCertChainBtn', () => this.verifyCertificateChain());
    }
    
    /**
     * Carrega o certificado CA da raiz se existir
     */
    loadCACertificate() {
        $.get('/pki/api/ca/certificate')
            .done((response) => {
                if (response.success) {
                    this.caCertificate = response.certificate;
                    this.showCAStatus(true);
                } else {
                    this.showCAStatus(false);
                }
            })
            .fail(() => {
                this.showCAStatus(false);
            });
    }
    
    /**
     * Gera certificado CA raiz
     */
    generateCACertificate() {
        if (!confirm('Gerar um novo Certificado de Autoridade (CA) invalidará todos os certificados existentes. Continuar?')) {
            return;
        }
        
        this.showProgress('Gerando Certificado CA...', 0);
        
        const caData = {
            commonName: 'SecureMessaging Root CA',
            organization: 'SecureMessaging',
            organizationalUnit: 'Certificate Authority',
            country: 'MZ',
            validityYears: 10,
            keySize: 2048
        };
        
        $.ajax({
            url: '/pki/api/ca/generate',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(caData),
            success: (response) => {
                if (response.success) {
                    this.showProgress('Certificado CA gerado com sucesso!', 100);
                    this.caCertificate = response.certificate;
                    this.showCAStatus(true);
                    this.showCertificateDetails(response.certificate, 'CA');
                    
                    setTimeout(() => {
                        location.reload();
                    }, 2000);
                } else {
                    this.showError('Erro ao gerar CA: ' + response.error);
                }
            },
            error: (xhr) => {
                this.showError('Erro na geração do CA: ' + (xhr.responseJSON?.error || 'Erro desconhecido'));
            }
        });
    }
    
    /**
     * Gera certificado auto-assinado para o usuário atual
     */
    generateSelfSigned() {
        this.showProgress('Gerando certificado auto-assinado...', 0);
        
        const certData = this.getCertificateFormData();
        certData.type = 'SELF_SIGNED';
        
        this.showProgress('Gerando par de chaves RSA 2048-bit...', 25);
        
        setTimeout(() => {
            this.showProgress('Criando estrutura do certificado...', 50);
            
            setTimeout(() => {
                $.ajax({
                    url: '/pki/api/certificates/generate-self-signed',
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(certData),
                    success: (response) => {
                        if (response.success) {
                            this.showProgress('Certificado gerado com sucesso!', 100);
                            this.showCertificateDetails(response.certificate, 'Auto-Assinado');
                            this.addCertificateToList(response.certificate);
                        } else {
                            this.showError('Erro: ' + response.error);
                        }
                    },
                    error: (xhr) => {
                        this.showError('Erro na geração: ' + (xhr.responseJSON?.error || 'Erro desconhecido'));
                    }
                });
            }, 1000);
        }, 1000);
    }
    
    /**
     * Gera certificado assinado pela CA
     */
    generateCASigned() {
        if (!this.caCertificate) {
            alert('É necessário gerar um Certificado CA primeiro!');
            return;
        }
        
        this.showProgress('Gerando certificado assinado pela CA...', 0);
        
        const certData = this.getCertificateFormData();
        certData.type = 'CA_SIGNED';
        
        this.showProgress('Gerando par de chaves RSA 2048-bit...', 20);
        
        setTimeout(() => {
            this.showProgress('Criando CSR (Certificate Signing Request)...', 40);
            
            setTimeout(() => {
                this.showProgress('Assinando certificado com CA...', 60);
                
                setTimeout(() => {
                    $.ajax({
                        url: '/pki/api/certificates/generate-ca-signed',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(certData),
                        success: (response) => {
                            if (response.success) {
                                this.showProgress('Certificado CA assinado com sucesso!', 100);
                                this.showCertificateDetails(response.certificate, 'Assinado pela CA');
                                this.addCertificateToList(response.certificate);
                            } else {
                                this.showError('Erro: ' + response.error);
                            }
                        },
                        error: (xhr) => {
                            this.showError('Erro na geração: ' + (xhr.responseJSON?.error || 'Erro desconhecido'));
                        }
                    });
                }, 800);
            }, 800);
        }, 800);
    }
    
    /**
     * Obtém dados do formulário de certificado
     */
    getCertificateFormData() {
        return {
            commonName: $('#certCommonName').val() || window.currentUser,
            organization: $('#certOrganization').val() || 'SecureMessaging',
            organizationalUnit: $('#certOU').val() || 'Users',
            locality: $('#certLocality').val() || 'Maputo',
            state: $('#certState').val() || 'Maputo',
            country: $('#certCountry').val() || 'MZ',
            email: $('#certEmail').val() || `${window.currentUser}@securemessaging.com`,
            validityYears: parseInt($('#certValidity').val()) || 1,
            keySize: parseInt($('#certKeySize').val()) || 2048
        };
    }
    
    /**
     * Visualiza detalhes do certificado
     */
    viewCertificate(event) {
        const certId = $(event.target).closest('[data-cert-id]').data('cert-id');
        
        $.get(`/pki/api/certificates/${certId}`)
            .done((response) => {
                if (response.success) {
                    this.showCertificateDetailsModal(response.certificate);
                }
            })
            .fail(() => {
                this.showError('Erro ao carregar certificado');
            });
    }
    
    /**
     * Exibe modal com detalhes do certificado
     */
    showCertificateDetailsModal(cert) {
        const modalHtml = `
            <div class="modal fade" id="certDetailsModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header bg-primary text-white">
                            <h5 class="modal-title">
                                <i class="fas fa-certificate"></i> Detalhes do Certificado
                            </h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            ${this.formatCertificateDetails(cert)}
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
                            <button type="button" class="btn btn-primary" onclick="window.certGenerator.exportCertificateById('${cert.id}')">
                                <i class="fas fa-download"></i> Exportar
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        $('#certDetailsModal').remove();
        $('body').append(modalHtml);
        $('#certDetailsModal').modal('show');
    }
    
    /**
     * Formata detalhes do certificado para exibição
     */
    formatCertificateDetails(cert) {
        const statusBadge = cert.status === 'VALID' ? 
            '<span class="badge bg-success">Válido</span>' : 
            '<span class="badge bg-danger">Inválido</span>';
        
        const typeBadge = cert.selfSigned ? 
            '<span class="badge bg-warning">Auto-Assinado</span>' : 
            '<span class="badge bg-primary">Assinado pela CA</span>';
        
        return `
            <div class="certificate-details">
                <div class="row mb-3">
                    <div class="col-md-6">
                        <strong>Status:</strong> ${statusBadge}
                    </div>
                    <div class="col-md-6">
                        <strong>Tipo:</strong> ${typeBadge}
                    </div>
                </div>
                
                <h6 class="border-bottom pb-2">Informações do Titular</h6>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <strong>Nome Comum (CN):</strong><br>
                        ${cert.commonName || 'N/A'}
                    </div>
                    <div class="col-md-6">
                        <strong>Email:</strong><br>
                        ${cert.email || 'N/A'}
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <strong>Organização (O):</strong><br>
                        ${cert.organization || 'N/A'}
                    </div>
                    <div class="col-md-6">
                        <strong>Unidade (OU):</strong><br>
                        ${cert.organizationalUnit || 'N/A'}
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-4">
                        <strong>Localidade:</strong><br>
                        ${cert.locality || 'N/A'}
                    </div>
                    <div class="col-md-4">
                        <strong>Estado:</strong><br>
                        ${cert.state || 'N/A'}
                    </div>
                    <div class="col-md-4">
                        <strong>País:</strong><br>
                        ${cert.country || 'N/A'}
                    </div>
                </div>
                
                <h6 class="border-bottom pb-2 mt-4">Validade</h6>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <strong>Válido De:</strong><br>
                        ${new Date(cert.validFrom).toLocaleString('pt-BR')}
                    </div>
                    <div class="col-md-6">
                        <strong>Válido Até:</strong><br>
                        ${new Date(cert.validTo).toLocaleString('pt-BR')}
                    </div>
                </div>
                
                <h6 class="border-bottom pb-2 mt-4">Informações Técnicas</h6>
                <div class="mb-2">
                    <strong>Número de Série:</strong><br>
                    <code>${cert.serialNumber || 'N/A'}</code>
                </div>
                <div class="mb-2">
                    <strong>Fingerprint (SHA-256):</strong><br>
                    <code style="word-break: break-all;">${cert.fingerprint || 'N/A'}</code>
                </div>
                <div class="mb-2">
                    <strong>Tamanho da Chave:</strong><br>
                    ${cert.keySize || 2048} bits
                </div>
                <div class="mb-2">
                    <strong>Algoritmo de Assinatura:</strong><br>
                    SHA256withRSA
                </div>
                
                ${!cert.selfSigned ? `
                    <h6 class="border-bottom pb-2 mt-4">Emissor (CA)</h6>
                    <div class="mb-2">
                        <strong>Assinado Por:</strong><br>
                        ${cert.issuerName || 'SecureMessaging Root CA'}
                    </div>
                ` : ''}
            </div>
        `;
    }
    
    /**
     * Exporta certificado em formato PEM
     */
    exportCertificate(event) {
        const certId = $(event.target).closest('[data-cert-id]').data('cert-id');
        this.exportCertificateById(certId);
    }
    
    exportCertificateById(certId) {
        $.get(`/pki/api/certificates/${certId}/export`)
            .done((response) => {
                if (response.success) {
                    this.downloadFile(
                        response.certificate, 
                        `certificate_${certId}.pem`, 
                        'application/x-pem-file'
                    );
                    
                    // Também exporta a chave privada se disponível
                    if (response.privateKey) {
                        this.downloadFile(
                            response.privateKey, 
                            `private_key_${certId}.pem`, 
                            'application/x-pem-file'
                        );
                    }
                }
            })
            .fail(() => {
                this.showError('Erro ao exportar certificado');
            });
    }
    
    /**
     * Revoga um certificado
     */
    revokeCertificate(event) {
        const certId = $(event.target).closest('[data-cert-id]').data('cert-id');
        
        if (!confirm('Tem certeza que deseja revogar este certificado?')) {
            return;
        }
        
        $.ajax({
            url: `/pki/api/certificates/${certId}/revoke`,
            method: 'POST',
            success: (response) => {
                if (response.success) {
                    this.showSuccess('Certificado revogado com sucesso');
                    location.reload();
                } else {
                    this.showError('Erro ao revogar: ' + response.error);
                }
            },
            error: (xhr) => {
                this.showError('Erro na revogação: ' + (xhr.responseJSON?.error || 'Erro desconhecido'));
            }
        });
    }
    
    /**
     * Verifica cadeia de certificados
     */
    verifyCertificateChain() {
        const certId = $('#verifyCertId').val();
        if (!certId) {
            alert('Selecione um certificado para verificar');
            return;
        }
        
        this.showProgress('Verificando cadeia de certificados...', 0);
        
        $.post(`/pki/api/certificates/verify-chain/${certId}`)
            .done((response) => {
                this.showProgress('Verificação concluída!', 100);
                this.showChainVerificationResult(response);
            })
            .fail((xhr) => {
                this.showError('Erro na verificação: ' + (xhr.responseJSON?.error || 'Erro desconhecido'));
            });
    }
    
    /**
     * Exibe resultado da verificação da cadeia
     */
    showChainVerificationResult(result) {
        const alertClass = result.valid ? 'alert-success' : 'alert-danger';
        const icon = result.valid ? 'fa-check-circle' : 'fa-times-circle';
        
        let chainHtml = '<h6>Cadeia de Certificados:</h6><ol>';
        result.chain.forEach(cert => {
            chainHtml += `<li>${cert.commonName} (${cert.type})</li>`;
        });
        chainHtml += '</ol>';
        
        const resultHtml = `
            <div class="alert ${alertClass}">
                <h6><i class="fas ${icon}"></i> ${result.valid ? 'Cadeia Válida' : 'Cadeia Inválida'}</h6>
                ${chainHtml}
                <p class="mb-0"><strong>Confiança:</strong> ${result.trustLevel}</p>
                ${result.warnings?.length ? `
                    <div class="mt-2">
                        <strong>Avisos:</strong>
                        <ul>
                            ${result.warnings.map(w => `<li>${w}</li>`).join('')}
                        </ul>
                    </div>
                ` : ''}
            </div>
        `;
        
        $('#chainVerificationResult').html(resultHtml);
    }
    
    /**
     * Adiciona certificado à lista
     */
    addCertificateToList(cert) {
        const certHtml = `
            <div class="card mb-2" data-cert-id="${cert.id}">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="mb-1">${cert.commonName}</h6>
                            <small class="text-muted">
                                ${cert.selfSigned ? 'Auto-Assinado' : 'Assinado pela CA'}
                                • Válido até ${new Date(cert.validTo).toLocaleDateString('pt-BR')}
                            </small>
                        </div>
                        <div>
                            <button class="btn btn-sm btn-info view-cert-btn">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="btn btn-sm btn-primary export-cert-btn">
                                <i class="fas fa-download"></i>
                            </button>
                            <button class="btn btn-sm btn-danger revoke-cert-btn">
                                <i class="fas fa-ban"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        $('#certificatesList').prepend(certHtml);
    }
    
    /**
     * Mostra detalhes do certificado gerado
     */
    showCertificateDetails(cert, type) {
        const detailsHtml = `
            <div class="alert alert-success">
                <h6><i class="fas fa-certificate"></i> Certificado ${type} Gerado com Sucesso!</h6>
                <div class="mt-3">
                    <strong>Titular:</strong> ${cert.commonName}<br>
                    <strong>Organização:</strong> ${cert.organization}<br>
                    <strong>Válido de:</strong> ${new Date(cert.validFrom).toLocaleDateString('pt-BR')}<br>
                    <strong>Válido até:</strong> ${new Date(cert.validTo).toLocaleDateString('pt-BR')}<br>
                    <strong>Fingerprint:</strong> <code>${cert.fingerprint?.substring(0, 32)}...</code>
                </div>
            </div>
        `;
        
        $('#certificateResult').html(detailsHtml);
    }
    
    /**
     * Mostra status da CA
     */
    showCAStatus(exists) {
        const statusHtml = exists ? `
            <div class="alert alert-success">
                <i class="fas fa-check-circle"></i> Certificado CA está ativo e válido
            </div>
        ` : `
            <div class="alert alert-warning">
                <i class="fas fa-exclamation-triangle"></i> Nenhum Certificado CA encontrado. 
                <button class="btn btn-sm btn-primary" id="generateCACertBtn">
                    Gerar Agora
                </button>
            </div>
        `;
        
        $('#caStatus').html(statusHtml);
    }
    
    /**
     * Funções auxiliares
     */
    showProgress(message, percentage) {
        const progressHtml = `
            <div class="progress mb-2">
                <div class="progress-bar progress-bar-striped progress-bar-animated" 
                     style="width: ${percentage}%"></div>
            </div>
            <small class="text-muted">${message}</small>
        `;
        
        $('#certificateProgress').html(progressHtml);
        
        if (percentage === 100) {
            setTimeout(() => {
                $('#certificateProgress').empty();
            }, 3000);
        }
    }
    
    showError(message) {
        const errorHtml = `
            <div class="alert alert-danger alert-dismissible fade show">
                <i class="fas fa-exclamation-triangle"></i> ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        $('#certificateProgress').html(errorHtml);
    }
    
    showSuccess(message) {
        const successHtml = `
            <div class="alert alert-success alert-dismissible fade show">
                <i class="fas fa-check-circle"></i> ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        $('#certificateResult').html(successHtml);
    }
    
    downloadFile(content, filename, mimeType) {
        const blob = new Blob([content], { type: mimeType });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }
}

// Inicializar quando o DOM estiver pronto
$(document).ready(() => {
    window.certGenerator = new CertificateGenerator();
});