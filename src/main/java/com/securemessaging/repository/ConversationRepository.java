package com.securemessaging.repository;

import com.securemessaging.model.entity.Conversation;
import com.securemessaging.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    @Query("SELECT c FROM Conversation c WHERE (c.user1 = :user1 AND c.user2 = :user2) OR " +
           "(c.user1 = :user2 AND c.user2 = :user1)")
    Optional<Conversation> findByUsers(User user1, User user2);
    
    @Query("SELECT c FROM Conversation c WHERE c.user1 = :user OR c.user2 = :user " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByUser(User user);
}
