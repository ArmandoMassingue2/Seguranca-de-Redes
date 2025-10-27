package com.securemessaging.controller;

import com.securemessaging.exception.CryptoException;
import com.securemessaging.model.dto.MessageDTO;
import com.securemessaging.model.entity.Message;
import com.securemessaging.model.enums.MessageType;
import com.securemessaging.service.MessageService;
import com.securemessaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            String recipient = payload.get("recipient");
            String content = payload.get("content");
            String messageTypeStr = payload.getOrDefault("messageType", "TEXT");
            
            MessageType messageType = MessageType.valueOf(messageTypeStr.toUpperCase());
            
            Message message = messageService.sendMessage(principal.getName(), recipient, content, messageType);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messageId", message.getId(),
                "timestamp", message.getTimestamp()
            ));
            
        } catch (CryptoException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Encryption failed: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/conversation/{otherUser}")
    public ResponseEntity<List<MessageDTO>> getConversation(@PathVariable String otherUser, Principal principal) {
        try {
            List<MessageDTO> messages = messageService.getConversationMessages(principal.getName(), otherUser);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/decrypt/{messageId}")
    public ResponseEntity<?> decryptMessage(@PathVariable Long messageId, Principal principal) {
        try {
            String decryptedContent = messageService.decryptMessage(messageId, principal.getName());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "content", decryptedContent
            ));
        } catch (CryptoException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Decryption failed: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/unread")
    public ResponseEntity<List<MessageDTO>> getUnreadMessages(Principal principal) {
        List<MessageDTO> unreadMessages = messageService.getUnreadMessages(principal.getName());
        return ResponseEntity.ok(unreadMessages);
    }
    
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        long count = messageService.getUnreadMessageCount(principal.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
