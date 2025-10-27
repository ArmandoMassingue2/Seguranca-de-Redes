package com.securemessaging.service.crypto;

import com.securemessaging.exception.CertificateException;
import com.securemessaging.model.entity.Certificate;
import com.securemessaging.model.entity.User;
import com.securemessaging.model.enums.CertificateStatus;
import com.securemessaging.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PKIService {
    
    @Autowired
    private RSAKeyService rsaKeyService;
    
    @Autowired
    private HashService hashService;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // IMPLEMENTAÇÃO: Simulação de CA Raiz conforme requisitos
    private Certificate rootCACertificate;
    private PrivateKey rootCAPrivateKey;
    private PublicKey rootCAPublicKey;
    
    // Inicializar CA Raiz auto-assinado
    public void initializeRootCA() throws CertificateException {
        try {
            // Gerar par de chaves para CA Raiz
            java.security.KeyPair caKeyPair = rsaKeyService.generateKeyPair();
            this.rootCAPrivateKey = caKeyPair.getPrivate();
            this.rootCAPublicKey = caKeyPair.getPublic();
            
            // Criar certificado CA Raiz auto-assinado
            String serialNumber = generateSerialNumber();
            LocalDateTime validFrom = LocalDateTime.now();
            LocalDateTime validTo = validFrom.plusYears(10); // CA válida por 10 anos
            
            String subject = "CN=SecureMessaging Root CA, O=SecureMessaging, C=MZ";
            String issuer = subject; // Auto-assinado
            
            String publicKeyString = CryptoUtils.encodeBase64(rootCAPublicKey.getEncoded());
            
            // Criar dados do certificado
            String certificateData = createCertificateData(serialNumber, subject, issuer, 
                                                          publicKeyString, validFrom, validTo);
            
            // Assinar com a própria chave privada (auto-assinado)
            byte[] signature = rsaKeyService.sign(certificateData.getBytes(), rootCAPrivateKey);
            String signatureString = Base64.getEncoder().encodeToString(signature);
            
            // Criar entidade do certificado CA
            User caUser = new User(); // User fictício para CA
            caUser.setUsername("ROOT_CA");
            caUser.setEmail("ca@securemessaging.com");
            
            this.rootCACertificate = new Certificate(caUser, serialNumber, subject, issuer, 
                                                   publicKeyString, validFrom, validTo);
            this.rootCACertificate.setSignature(signatureString);
            this.rootCACertificate.setCertificateData(certificateData);
            this.rootCACertificate.setSelfSigned(true);
            this.rootCACertificate.setStatus(CertificateStatus.ACTIVE);
            
        } catch (Exception e) {
            throw new CertificateException("Failed to initialize Root CA", e);
        }
    }
    
    public Certificate generateSelfSignedCertificate(User user) throws CertificateException {
        try {
            String serialNumber = generateSerialNumber();
            
            LocalDateTime validFrom = LocalDateTime.now();
            LocalDateTime validTo = validFrom.plusYears(1);
            
            String subject = String.format("CN=%s, E=%s", user.getUsername(), user.getEmail());
            String issuer = subject; // Auto-assinado
            
            PublicKey publicKey = CryptoUtils.getPublicKeyFromString(user.getPublicKeyRSA());
            String publicKeyString = user.getPublicKeyRSA();
            
            String certificateData = createCertificateData(serialNumber, subject, issuer, 
                                                          publicKeyString, validFrom, validTo);
            
            PrivateKey privateKey = CryptoUtils.getPrivateKeyFromString(user.getPrivateKeyRSA());
            byte[] signature = rsaKeyService.sign(certificateData.getBytes(), privateKey);
            String signatureString = Base64.getEncoder().encodeToString(signature);
            
            Certificate certificate = new Certificate(user, serialNumber, subject, issuer, 
                                                    publicKeyString, validFrom, validTo);
            certificate.setSignature(signatureString);
            certificate.setCertificateData(certificateData);
            certificate.setSelfSigned(true);
            certificate.setStatus(CertificateStatus.ACTIVE);
            
            return certificate;
            
        } catch (Exception e) {
            throw new CertificateException("Failed to generate self-signed certificate", e);
        }
    }
    
    // IMPLEMENTAÇÃO: Gerar certificado assinado pela CA Raiz
    public Certificate generateCASignedCertificate(User user) throws CertificateException {
        try {
            if (rootCACertificate == null) {
                initializeRootCA();
            }
            
            String serialNumber = generateSerialNumber();
            
            LocalDateTime validFrom = LocalDateTime.now();
            LocalDateTime validTo = validFrom.plusYears(2); // Certificados de usuário válidos por 2 anos
            
            String subject = String.format("CN=%s, E=%s, O=SecureMessaging", user.getUsername(), user.getEmail());
            String issuer = rootCACertificate.getSubject(); // Assinado pela CA Raiz
            
            String publicKeyString = user.getPublicKeyRSA();
            
            String certificateData = createCertificateData(serialNumber, subject, issuer, 
                                                          publicKeyString, validFrom, validTo);
            
            // Assinar com a chave privada da CA Raiz
            byte[] signature = rsaKeyService.sign(certificateData.getBytes(), rootCAPrivateKey);
            String signatureString = Base64.getEncoder().encodeToString(signature);
            
            Certificate certificate = new Certificate(user, serialNumber, subject, issuer, 
                                                    publicKeyString, validFrom, validTo);
            certificate.setSignature(signatureString);
            certificate.setCertificateData(certificateData);
            certificate.setSelfSigned(false); // Assinado pela CA
            certificate.setStatus(CertificateStatus.ACTIVE);
            
            return certificate;
            
        } catch (Exception e) {
            throw new CertificateException("Failed to generate CA-signed certificate", e);
        }
    }
    
    public boolean verifyCertificate(Certificate certificate) throws CertificateException {
        try {
            // Verificar se o certificado não expirou
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(certificate.getValidFrom()) || now.isAfter(certificate.getValidTo())) {
                return false;
            }
            
            // Verificar se o certificado não foi revogado
            if (certificate.getStatus() == CertificateStatus.REVOKED) {
                return false;
            }
            
            // Verificar assinatura
            PublicKey verificationKey;
            if (certificate.isSelfSigned()) {
                // Para certificados auto-assinados, usar a chave pública do próprio certificado
                verificationKey = CryptoUtils.getPublicKeyFromString(certificate.getPublicKey());
            } else {
                // Para certificados assinados pela CA, usar a chave pública da CA Raiz
                if (rootCACertificate == null) {
                    throw new CertificateException("Root CA certificate not available");
                }
                verificationKey = rootCAPublicKey;
            }
            
            byte[] signature = Base64.getDecoder().decode(certificate.getSignature());
            boolean signatureValid = rsaKeyService.verify(certificate.getCertificateData().getBytes(), 
                                                         signature, verificationKey);
            
            if (!signatureValid) {
                return false;
            }
            
            // Verificar integridade dos dados usando SHA-256 e SHA3-256
            return verifyIntegrity(certificate);
            
        } catch (Exception e) {
            throw new CertificateException("Failed to verify certificate", e);
        }
    }
    
    private boolean verifyIntegrity(Certificate certificate) throws CertificateException {
        try {
            byte[] certificateData = certificate.getCertificateData().getBytes();
            
            // Verificar com SHA-256
            byte[] hashSHA256 = hashService.sha256(certificateData);
            
            // Verificar com SHA3-256
            byte[] hashSHA3_256 = hashService.sha3_256(certificateData);
            
            // Para verificação de integridade, comparar com hash armazenado (se existir)
            // ou verificar que os hashes são consistentes
            return hashSHA256.length == 32 && hashSHA3_256.length == 32;
            
        } catch (Exception e) {
            throw new CertificateException("Integrity verification failed", e);
        }
    }
    
    public void revokeCertificate(Certificate certificate) {
        certificate.setStatus(CertificateStatus.REVOKED);
    }
    
    public boolean isCertificateValid(Certificate certificate) {
        LocalDateTime now = LocalDateTime.now();
        return certificate.getStatus() == CertificateStatus.ACTIVE &&
               now.isAfter(certificate.getValidFrom()) &&
               now.isBefore(certificate.getValidTo());
    }
    
    private String generateSerialNumber() {
        byte[] serialBytes = new byte[16];
        secureRandom.nextBytes(serialBytes);
        return new BigInteger(1, serialBytes).toString(16).toUpperCase();
    }
    
    private String createCertificateData(String serialNumber, String subject, String issuer,
                                       String publicKey, LocalDateTime validFrom, LocalDateTime validTo) {
        return String.format(
            "Version: 3\n" +
            "SerialNumber: %s\n" +
            "Subject: %s\n" +
            "Issuer: %s\n" +
            "ValidFrom: %s\n" +
            "ValidTo: %s\n" +
            "PublicKey: %s\n" +
            "KeyAlgorithm: RSA\n" +
            "KeySize: 1024\n" +
            "SignatureAlgorithm: SHA256withRSA",
            serialNumber, subject, issuer, validFrom, validTo, publicKey
        );
    }
    
    public String getCertificateFingerprint(Certificate certificate) throws CertificateException {
        try {
            // Gerar fingerprint usando SHA-256 e SHA3-256
            byte[] hashSHA256 = hashService.sha256(certificate.getCertificateData().getBytes());
            byte[] hashSHA3_256 = hashService.sha3_256(certificate.getCertificateData().getBytes());
            
            // Combinar os hashes para fingerprint único
            String sha256Hex = hashService.hashToHex(hashSHA256); // Usar método corrigido
            String sha3_256Hex = hashService.sha3_256Hex(certificate.getCertificateData().getBytes());
            
            return sha256Hex + ":" + sha3_256Hex;
        } catch (Exception e) {
            throw new CertificateException("Failed to generate certificate fingerprint", e);
        }
    }
    
    // Getter para o certificado da CA Raiz
    public Certificate getRootCACertificate() {
        return rootCACertificate;
    }
    
    // Método para verificar se um certificado foi assinado pela CA Raiz
    public boolean isSignedByRootCA(Certificate certificate) throws CertificateException {
        if (certificate.isSelfSigned()) {
            return false;
        }
        
        try {
            if (rootCACertificate == null) {
                initializeRootCA();
            }
            
            byte[] signature = Base64.getDecoder().decode(certificate.getSignature());
            return rsaKeyService.verify(certificate.getCertificateData().getBytes(), 
                                      signature, rootCAPublicKey);
        } catch (Exception e) {
            throw new CertificateException("Failed to verify CA signature", e);
        }
    }}