package com.securemessaging.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "public_key_rsa", columnDefinition = "TEXT")
    private String publicKeyRSA;
    
    @Column(name = "private_key_rsa", columnDefinition = "TEXT")
    private String privateKeyRSA;
    
    @Column(name = "public_key_dh", columnDefinition = "TEXT")
    private String publicKeyDH;
    
    @Column(name = "private_key_dh", columnDefinition = "TEXT")
    private String privateKeyDH;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<com.securemessaging.model.entity.KeyPair> keyPairs;
    
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<com.securemessaging.model.entity.Certificate> certificates;
    
    // Constructors
    public User() {
        // Construtor vazio para JPA
    }
    
    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
    }
    
    // Callback method executed before persisting
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    
    // Callback method executed before updating
    @PreUpdate
    protected void onUpdate() {
        // Se necessário, pode adicionar lógica aqui
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getPublicKeyRSA() { return publicKeyRSA; }
    public void setPublicKeyRSA(String publicKeyRSA) { this.publicKeyRSA = publicKeyRSA; }
    
    public String getPrivateKeyRSA() { return privateKeyRSA; }
    public void setPrivateKeyRSA(String privateKeyRSA) { this.privateKeyRSA = privateKeyRSA; }
    
    public String getPublicKeyDH() { return publicKeyDH; }
    public void setPublicKeyDH(String publicKeyDH) { this.publicKeyDH = publicKeyDH; }
    
    public String getPrivateKeyDH() { return privateKeyDH; }
    public void setPrivateKeyDH(String privateKeyDH) { this.privateKeyDH = privateKeyDH; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public List<com.securemessaging.model.entity.KeyPair> getKeyPairs() { return keyPairs; }
    public void setKeyPairs(List<com.securemessaging.model.entity.KeyPair> keyPairs) { this.keyPairs = keyPairs; }
    
    public List<com.securemessaging.model.entity.Certificate> getCertificates() { return certificates; }
    public void setCertificates(List<com.securemessaging.model.entity.Certificate> certificates) { this.certificates = certificates; }
}