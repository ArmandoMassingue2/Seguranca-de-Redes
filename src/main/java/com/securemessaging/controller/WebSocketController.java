package com.securemessaging.controller;

import com.securemessaging.model.dto.MessageDTO;
import com.securemessaging.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class WebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private MessageService messageService;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(MessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        // Set sender from authenticated user
        messageDTO.setSenderUsername(principal.getName());
        messageDTO.setTimestamp(LocalDateTime.now());
        
        try {
            System.out.println("WebSocket: Enviando mensagem de " + principal.getName() + 
                             " para " + messageDTO.getRecipientUsername());
            
            // CORREÇÃO: Se tiver ID da mensagem, buscar conteúdo descriptografado
            if (messageDTO.getId() != null) {
                try {
                    String decryptedContent = messageService.decryptMessage(
                        messageDTO.getId(), messageDTO.getRecipientUsername());
                    messageDTO.setContent(decryptedContent);
                    System.out.println("WebSocket: Mensagem descriptografada com sucesso");
                } catch (Exception e) {
                    System.err.println("WebSocket: Erro ao descriptografar para " + 
                                     messageDTO.getRecipientUsername() + ": " + e.getMessage());
                    // Se falhar para o destinatário, tentar para o remetente
                    try {
                        String decryptedContent = messageService.decryptMessage(
                            messageDTO.getId(), principal.getName());
                        messageDTO.setContent(decryptedContent);
                    } catch (Exception e2) {
                        System.err.println("WebSocket: Erro ao descriptografar para remetente: " + e2.getMessage());
                        messageDTO.setContent("[Erro na descriptografia]");
                    }
                }
            }
            
            // Send to specific recipient
            messagingTemplate.convertAndSendToUser(
                messageDTO.getRecipientUsername(),
                "/queue/messages",
                messageDTO
            );
            
            // Also send to sender for confirmation
            messagingTemplate.convertAndSendToUser(
                messageDTO.getSenderUsername(),
                "/queue/messages",
                messageDTO
            );
            
        } catch (Exception e) {
            System.err.println("WebSocket: Erro ao enviar mensagem: " + e.getMessage());
            e.printStackTrace();
            
            // Send error message
            MessageDTO errorMsg = new MessageDTO();
            errorMsg.setSenderUsername("System");
            errorMsg.setRecipientUsername(messageDTO.getRecipientUsername());
            errorMsg.setContent("Erro ao processar mensagem: " + e.getMessage());
            errorMsg.setTimestamp(LocalDateTime.now());
            
            messagingTemplate.convertAndSendToUser(
                messageDTO.getRecipientUsername(),
                "/queue/messages",
                errorMsg
            );
        }
    }
    
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public MessageDTO addUser(MessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", messageDTO.getSenderUsername());
        System.out.println("WebSocket: Usuário conectado: " + messageDTO.getSenderUsername());
        return messageDTO;
    }
    
    @MessageMapping("/typing")
    public void handleTyping(String recipientUsername, Principal principal) {
        System.out.println("WebSocket: " + principal.getName() + " está digitando para " + recipientUsername);
        messagingTemplate.convertAndSendToUser(
            recipientUsername,
            "/queue/typing",
            principal.getName() + " is typing..."
        );
    }
}