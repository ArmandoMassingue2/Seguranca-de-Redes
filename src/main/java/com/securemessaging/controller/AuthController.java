// ===================== AuthController.java =====================
package com.securemessaging.controller;

import com.securemessaging.exception.CryptoException;
import com.securemessaging.model.entity.User;
import com.securemessaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal; 
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/chat";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@RequestParam String username,
                                      @RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/register";
            }

            if (password.length() < 8) {
                redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
                return "redirect:/register";
            }

            userService.registerUser(username, email, password);
            redirectAttributes.addFlashAttribute("success", "Registration successful! RSA keys and certificate generated.");
            return "redirect:/login";

        } catch (CryptoException e) {
            redirectAttributes.addFlashAttribute("error", "Cryptographic error: " + e.getMessage());
            return "redirect:/register";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        Optional<User> userOpt = userService.findByUsername(principal.getName());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            model.addAttribute("publicKeyRSA", user.getPublicKeyRSA());
            return "auth/profile";
        }
        return "redirect:/login";
    }
}
