package com.securemessaging.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastMessageAt;
    
    @Column(columnDefinition = "TEXT")
    private String sharedSecretHash;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<Message> messages;
    
    // Constructors
    public Conversation() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Conversation(User user1, User user2) {
        this();
        this.user1 = user1;
        this.user2 = user2;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser1() { return user1; }
    public void setUser1(User user1) { this.user1 = user1; }
    
    public User getUser2() { return user2; }
    public void setUser2(User user2) { this.user2 = user2; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    
    public String getSharedSecretHash() { return sharedSecretHash; }
    public void setSharedSecretHash(String sharedSecretHash) { this.sharedSecretHash = sharedSecretHash; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}