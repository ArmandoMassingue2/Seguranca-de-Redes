package com.securemessaging.controller;

import com.securemessaging.model.dto.UserDTO;
import com.securemessaging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllActiveUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{username}/publickey")
    public ResponseEntity<String> getUserPublicKey(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(user.getPublicKeyRSA()))
                .orElse(ResponseEntity.notFound().build());
    }
}
