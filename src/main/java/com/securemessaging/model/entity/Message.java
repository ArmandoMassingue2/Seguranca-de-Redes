package com.securemessaging.model.entity;

import com.securemessaging.model.enums.MessageType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    
    @Column(columnDefinition = "LONGTEXT")
    private String encryptedContent;
    
    @Column(columnDefinition = "TEXT")
    private String encryptedSessionKey;
    
    @Column(columnDefinition = "TEXT")
    private String digitalSignature;
    
    @Column(columnDefinition = "TEXT")
    private String messageHash;
    
    @Enumerated(EnumType.STRING)
    private MessageType messageType;
    
    @Column
    private String fileName;
    
    @Column
    private Long fileSize;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private boolean delivered = false;
    
    @Column(name = "read_status", nullable = false)
    private boolean read = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    
    // Constructors
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Message(User sender, User recipient, String encryptedContent, MessageType messageType) {
        this();
        this.sender = sender;
        this.recipient = recipient;
        this.encryptedContent = encryptedContent;
        this.messageType = messageType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    
    public String getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(String encryptedContent) { this.encryptedContent = encryptedContent; }
    
    public String getEncryptedSessionKey() { return encryptedSessionKey; }
    public void setEncryptedSessionKey(String encryptedSessionKey) { this.encryptedSessionKey = encryptedSessionKey; }
    
    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }
    
    public String getMessageHash() { return messageHash; }
    public void setMessageHash(String messageHash) { this.messageHash = messageHash; }
    
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
}
