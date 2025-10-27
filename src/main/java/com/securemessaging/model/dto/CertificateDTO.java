package com.securemessaging.model.dto;

import com.securemessaging.model.enums.CertificateStatus;
import java.time.LocalDateTime;

public class CertificateDTO {
    private Long id;
    private String ownerUsername;
    private String serialNumber;
    private String subject;
    private String issuer;
    private String publicKey;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private CertificateStatus status;
    private boolean selfSigned;
    private String fingerprint;
    
    // Constructors
    public CertificateDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    
    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }
    
    public boolean isSelfSigned() { return selfSigned; }
    public void setSelfSigned(boolean selfSigned) { this.selfSigned = selfSigned; }
    
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
}