// ===================== ChatController.java =====================
package com.securemessaging.controller;

import com.securemessaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class ChatController {

    @Autowired
    private UserService userService;

    @GetMapping("/chat")
    public String chat(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("currentUser", principal.getName());
        model.addAttribute("users", userService.getAllActiveUsers());
        return "message/chat";
    }

    @GetMapping("/contacts")
    public String contacts(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("currentUser", principal.getName());
        model.addAttribute("users", userService.getAllActiveUsers());
        return "message/contacts";
    }

    @GetMapping("/encryption-status")
    public String encryptionStatus(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("currentUser", principal.getName());
        return "message/encryption-status";
    }
}
