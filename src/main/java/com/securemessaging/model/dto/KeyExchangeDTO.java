package com.securemessaging.model.dto;

public class KeyExchangeDTO {
    private String username;
    private String publicKeyRSA;
    private String publicKeyDH;
    private String dhParameters;
    private String sessionId;
    
    // Constructors
    public KeyExchangeDTO() {}
    
    public KeyExchangeDTO(String username, String publicKeyRSA, String publicKeyDH) {
        this.username = username;
        this.publicKeyRSA = publicKeyRSA;
        this.publicKeyDH = publicKeyDH;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPublicKeyRSA() { return publicKeyRSA; }
    public void setPublicKeyRSA(String publicKeyRSA) { this.publicKeyRSA = publicKeyRSA; }
    
    public String getPublicKeyDH() { return publicKeyDH; }
    public void setPublicKeyDH(String publicKeyDH) { this.publicKeyDH = publicKeyDH; }
    
    public String getDhParameters() { return dhParameters; }
    public void setDhParameters(String dhParameters) { this.dhParameters = dhParameters; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
