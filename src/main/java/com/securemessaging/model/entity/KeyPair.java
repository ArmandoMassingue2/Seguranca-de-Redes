package com.securemessaging.model.entity;

import com.securemessaging.model.enums.EncryptionAlgorithm;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "key_pairs")
public class KeyPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String publicKey;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String privateKey;
    
    @Enumerated(EnumType.STRING)
    private EncryptionAlgorithm algorithm;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private boolean active = true;
    
    // Constructors
    public KeyPair() {
        this.createdAt = LocalDateTime.now();
    }
    
    public KeyPair(User user, String publicKey, String privateKey, EncryptionAlgorithm algorithm) {
        this();
        this.user = user;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.algorithm = algorithm;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    
    public EncryptionAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(EncryptionAlgorithm algorithm) { this.algorithm = algorithm; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
