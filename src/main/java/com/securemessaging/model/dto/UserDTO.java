package com.securemessaging.model.dto;

import java.time.LocalDateTime;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String publicKeyRSA;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean active;
    
    // Constructors
    public UserDTO() {}
    
    public UserDTO(String username, String email) {
        this.username = username;
        this.email = email;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPublicKeyRSA() { return publicKeyRSA; }
    public void setPublicKeyRSA(String publicKeyRSA) { this.publicKeyRSA = publicKeyRSA; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}