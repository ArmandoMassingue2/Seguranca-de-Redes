package com.securemessaging.service;

import com.securemessaging.exception.KeyExchangeException;
import com.securemessaging.model.dto.KeyExchangeDTO;
import com.securemessaging.model.entity.User;
import com.securemessaging.repository.UserRepository;
import com.securemessaging.service.crypto.DiffieHellmanService;
import com.securemessaging.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class KeyExchangeService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DiffieHellmanService dhService;
    
    // In-memory storage for active key exchange sessions
    private final Map<String, KeyExchangeSession> activeSessions = new HashMap<>();
    
    public String initiateKeyExchange(String initiatorUsername, String recipientUsername) 
            throws KeyExchangeException {
        
        User initiator = userRepository.findByUsername(initiatorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Initiator not found"));
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        // Generate DH key pair for this session
        KeyPair dhKeyPair = dhService.generateDHKeyPair();
        
        // Create session
        String sessionId = UUID.randomUUID().toString();
        KeyExchangeSession session = new KeyExchangeSession(initiator, recipient, dhKeyPair);
        activeSessions.put(sessionId, session);
        
        return sessionId;
    }
    
    public KeyExchangeDTO getPublicKeyInfo(String sessionId, String username) throws KeyExchangeException {
        KeyExchangeSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new KeyExchangeException("Invalid session ID");
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        KeyExchangeDTO dto = new KeyExchangeDTO();
        dto.setUsername(username);
        dto.setPublicKeyRSA(user.getPublicKeyRSA());
        dto.setSessionId(sessionId);
        
        // Add DH public key for this session
        if (session.getInitiator().getUsername().equals(username)) {
            dto.setPublicKeyDH(CryptoUtils.encodeBase64(session.getInitiatorDHKeyPair().getPublic().getEncoded()));
        } else if (session.getRecipient().getUsername().equals(username)) {
            // Generate DH key pair for recipient if not exists
            if (session.getRecipientDHKeyPair() == null) {
                KeyPair recipientDHKeyPair = dhService.generateDHKeyPair();
                session.setRecipientDHKeyPair(recipientDHKeyPair);
            }
            dto.setPublicKeyDH(CryptoUtils.encodeBase64(session.getRecipientDHKeyPair().getPublic().getEncoded()));
        }
        
        return dto;
    }
    
    public SecretKey completeKeyExchange(String sessionId, String username) throws KeyExchangeException {
        KeyExchangeSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new KeyExchangeException("Invalid session ID");
        }
        
        try {
            SecretKey sharedSecret;
            
            if (session.getInitiator().getUsername().equals(username)) {
                // Initiator completes the exchange
                PrivateKey initiatorPrivateKey = session.getInitiatorDHKeyPair().getPrivate();
                PublicKey recipientPublicKey = session.getRecipientDHKeyPair().getPublic();
                sharedSecret = dhService.generateSharedSecret(initiatorPrivateKey, recipientPublicKey);
            } else if (session.getRecipient().getUsername().equals(username)) {
                // Recipient completes the exchange
                PrivateKey recipientPrivateKey = session.getRecipientDHKeyPair().getPrivate();
                PublicKey initiatorPublicKey = session.getInitiatorDHKeyPair().getPublic();
                sharedSecret = dhService.generateSharedSecret(recipientPrivateKey, initiatorPublicKey);
            } else {
                throw new KeyExchangeException("User not part of this key exchange session");
            }
            
            // Store shared secret in session
            session.setSharedSecret(sharedSecret);
            
            // Clean up session after successful exchange
            activeSessions.remove(sessionId);
            
            return sharedSecret;
            
        } catch (Exception e) {
            throw new KeyExchangeException("Failed to complete key exchange", e);
        }
    }
    
    public void cancelKeyExchange(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    // Inner class for key exchange session management
    private static class KeyExchangeSession {
        private final User initiator;
        private final User recipient;
        private final KeyPair initiatorDHKeyPair;
        private KeyPair recipientDHKeyPair;
        private SecretKey sharedSecret;
        
        public KeyExchangeSession(User initiator, User recipient, KeyPair initiatorDHKeyPair) {
            this.initiator = initiator;
            this.recipient = recipient;
            this.initiatorDHKeyPair = initiatorDHKeyPair;
        }
        
        // Getters and Setters
        public User getInitiator() { return initiator; }
        public User getRecipient() { return recipient; }
        public KeyPair getInitiatorDHKeyPair() { return initiatorDHKeyPair; }
        public KeyPair getRecipientDHKeyPair() { return recipientDHKeyPair; }
        public void setRecipientDHKeyPair(KeyPair recipientDHKeyPair) { this.recipientDHKeyPair = recipientDHKeyPair; }
        public SecretKey getSharedSecret() { return sharedSecret; }
        public void setSharedSecret(SecretKey sharedSecret) { this.sharedSecret = sharedSecret; }
    }
}