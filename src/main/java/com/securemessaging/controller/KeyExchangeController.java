package com.securemessaging.controller;

import com.securemessaging.exception.KeyExchangeException;
import com.securemessaging.model.dto.KeyExchangeDTO;
import com.securemessaging.service.KeyExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/keyexchange")
public class KeyExchangeController {
    
    @Autowired
    private KeyExchangeService keyExchangeService;
    
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateKeyExchange(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            String recipientUsername = payload.get("recipient");
            String sessionId = keyExchangeService.initiateKeyExchange(principal.getName(), recipientUsername);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionId", sessionId
            ));
        } catch (KeyExchangeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/publickey/{sessionId}")
    public ResponseEntity<?> getPublicKeyInfo(@PathVariable String sessionId, Principal principal) {
        try {
            KeyExchangeDTO keyInfo = keyExchangeService.getPublicKeyInfo(sessionId, principal.getName());
            return ResponseEntity.ok(keyInfo);
        } catch (KeyExchangeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/complete/{sessionId}")
    public ResponseEntity<?> completeKeyExchange(@PathVariable String sessionId, Principal principal) {
        try {
            SecretKey sharedSecret = keyExchangeService.completeKeyExchange(sessionId, principal.getName());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Key exchange completed successfully",
                "keyLength", sharedSecret.getEncoded().length * 8 // bits
            ));
        } catch (KeyExchangeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/cancel/{sessionId}")
    public ResponseEntity<?> cancelKeyExchange(@PathVariable String sessionId) {
        keyExchangeService.cancelKeyExchange(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
