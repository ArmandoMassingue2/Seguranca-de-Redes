package com.securemessaging.model.entity;

import com.securemessaging.model.enums.CertificateStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(nullable = false)
    private String issuer;
    
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String signature;
    
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;
    
    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;
    
    @Enumerated(EnumType.STRING)
    private CertificateStatus status = CertificateStatus.ACTIVE;
    
    @Column(name = "self_signed", nullable = false)
    private boolean selfSigned = true;
    
    @Column(name = "certificate_data", columnDefinition = "LONGTEXT")
    private String certificateData;
    
    // Constructors
    public Certificate() {}
    
    public Certificate(User owner, String serialNumber, String subject, String issuer, 
                      String publicKey, LocalDateTime validFrom, LocalDateTime validTo) {
        this.owner = owner;
        this.serialNumber = serialNumber;
        this.subject = subject;
        this.issuer = issuer;
        this.publicKey = publicKey;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    
    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }
    
    public boolean isSelfSigned() { return selfSigned; }
    public void setSelfSigned(boolean selfSigned) { this.selfSigned = selfSigned; }
    
    public String getCertificateData() { return certificateData; }
    public void setCertificateData(String certificateData) { this.certificateData = certificateData; }
}
