package com.securemessaging.service;

import com.securemessaging.exception.CryptoException;
import com.securemessaging.model.entity.Message;
import com.securemessaging.model.entity.User;
import com.securemessaging.model.entity.Conversation;
import com.securemessaging.model.dto.MessageDTO;
import com.securemessaging.model.enums.MessageType;
import com.securemessaging.repository.MessageRepository;
import com.securemessaging.repository.ConversationRepository;
import com.securemessaging.repository.UserRepository;
import com.securemessaging.service.crypto.PGPService;
import com.securemessaging.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PGPService pgpService;
    
    public Message sendMessage(String senderUsername, String recipientUsername, 
                              String content, MessageType messageType) throws CryptoException {
        
        // Find users
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        try {
            // Get keys
            PublicKey recipientPublicKey = CryptoUtils.getPublicKeyFromString(recipient.getPublicKeyRSA());
            PrivateKey senderPrivateKey = CryptoUtils.getPrivateKeyFromString(sender.getPrivateKeyRSA());
            
            // Encrypt message using PGP
            PGPService.PGPEncryptedMessage encryptedMessage = pgpService.encrypt(
                content.getBytes(), recipientPublicKey, senderPrivateKey);
            
            // Create message entity
            Message message = new Message(sender, recipient, 
                Base64.getEncoder().encodeToString(encryptedMessage.getEncryptedMessage()), messageType);
            
            message.setEncryptedSessionKey(Base64.getEncoder().encodeToString(encryptedMessage.getEncryptedSessionKey()));
            message.setDigitalSignature(Base64.getEncoder().encodeToString(encryptedMessage.getSignature()));
            message.setMessageHash(Base64.getEncoder().encodeToString(encryptedMessage.getMessageHash()));
            
            // Find or create conversation
            Optional<Conversation> conversationOpt = conversationRepository.findByUsers(sender, recipient);
            Conversation conversation;
            if (conversationOpt.isPresent()) {
                conversation = conversationOpt.get();
                conversation.setLastMessageAt(LocalDateTime.now());
            } else {
                conversation = new Conversation(sender, recipient);
                conversation.setLastMessageAt(LocalDateTime.now());
                conversation = conversationRepository.save(conversation);
            }
            
            message.setConversation(conversation);
            
            // Save message
            return messageRepository.save(message);
            
        } catch (Exception e) {
            throw new CryptoException("Failed to send encrypted message", e);
        }
    }
    
    public String decryptMessage(Long messageId, String recipientUsername) throws CryptoException {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        // Verify recipient
        if (!message.getRecipient().getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Unauthorized to decrypt this message");
        }
        
        try {
            // Get keys
            PrivateKey recipientPrivateKey = CryptoUtils.getPrivateKeyFromString(recipient.getPrivateKeyRSA());
            PublicKey senderPublicKey = CryptoUtils.getPublicKeyFromString(message.getSender().getPublicKeyRSA());
            
            // Reconstruct PGP message
            PGPService.PGPEncryptedMessage pgpMessage = new PGPService.PGPEncryptedMessage(
                Base64.getDecoder().decode(message.getEncryptedContent()),
                Base64.getDecoder().decode(message.getEncryptedSessionKey()),
                Base64.getDecoder().decode(message.getDigitalSignature()),
                Base64.getDecoder().decode(message.getMessageHash())
            );
            
            // Decrypt message
            byte[] decryptedContent = pgpService.decrypt(pgpMessage, recipientPrivateKey, senderPublicKey);
            
            // Mark as read
            if (!message.isRead()) {
                message.setRead(true);
                messageRepository.save(message);
            }
            
            return new String(decryptedContent);
            
        } catch (Exception e) {
            throw new CryptoException("Failed to decrypt message", e);
        }
    }
    
    public List<MessageDTO> getConversationMessages(String username1, String username2) {
        User user1 = userRepository.findByUsername(username1)
                .orElseThrow(() -> new IllegalArgumentException("User1 not found"));
        User user2 = userRepository.findByUsername(username2)
                .orElseThrow(() -> new IllegalArgumentException("User2 not found"));
        
        return messageRepository.findConversationMessages(user1, user2).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<MessageDTO> getUnreadMessages(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return messageRepository.findUnreadMessages(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public long getUnreadMessageCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return messageRepository.countUnreadMessages(user);
    }
    
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setRecipientUsername(message.getRecipient().getUsername());
        // Note: Content is encrypted, will be decrypted separately
        dto.setContent("[Encrypted]");
        dto.setMessageType(message.getMessageType());
        dto.setFileName(message.getFileName());
        dto.setFileSize(message.getFileSize());
        dto.setTimestamp(message.getTimestamp());
        dto.setDelivered(message.isDelivered());
        dto.setRead(message.isRead());
        return dto;
    }
}
