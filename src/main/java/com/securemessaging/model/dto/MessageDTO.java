package com.securemessaging.model.dto;

import com.securemessaging.model.enums.MessageType;
import java.time.LocalDateTime;

public class MessageDTO {
    private Long id;
    private String senderUsername;
    private String recipientUsername;
    private String content;
    private MessageType messageType;
    private String fileName;
    private Long fileSize;
    private LocalDateTime timestamp;
    private boolean delivered;
    private boolean read;
    
    // Constructors
    public MessageDTO() {}
    
    public MessageDTO(String senderUsername, String recipientUsername, String content, MessageType messageType) {
        this.senderUsername = senderUsername;
        this.recipientUsername = recipientUsername;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    
    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
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
}
