package com.securemessaging.repository;

import com.securemessaging.model.entity.Message;
import com.securemessaging.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.recipient = :user2) OR " +
           "(m.sender = :user2 AND m.recipient = :user1) ORDER BY m.timestamp ASC")
    List<Message> findConversationMessages(User user1, User user2);
    
    @Query("SELECT m FROM Message m WHERE m.recipient = :user AND m.delivered = false")
    List<Message> findUndeliveredMessages(User user);
    
    @Query("SELECT m FROM Message m WHERE m.recipient = :user AND m.read = false")
    List<Message> findUnreadMessages(User user);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient = :user AND m.read = false")
    long countUnreadMessages(User user);
}
