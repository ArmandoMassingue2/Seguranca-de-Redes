package com.securemessaging.service;

import com.securemessaging.exception.CryptoException;

import com.securemessaging.model.entity.User;
import com.securemessaging.model.entity.Certificate;
import com.securemessaging.model.dto.UserDTO;
import com.securemessaging.repository.UserRepository;
import com.securemessaging.repository.CertificateRepository;
import com.securemessaging.service.crypto.RSAKeyService;
import com.securemessaging.service.crypto.PKIService;
import com.securemessaging.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CertificateRepository certificateRepository;
    
    @Autowired
    private RSAKeyService rsaKeyService;
    
    @Autowired
    private PKIService pkiService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(String username, String email, String password) throws CryptoException {
        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create user
        User user = new User(username, email, passwordEncoder.encode(password));
        
        // Generate RSA key pair
        KeyPair rsaKeyPair = rsaKeyService.generateKeyPair();
        user.setPublicKeyRSA(CryptoUtils.encodeBase64(rsaKeyPair.getPublic().getEncoded()));
        user.setPrivateKeyRSA(CryptoUtils.encodeBase64(rsaKeyPair.getPrivate().getEncoded()));
        
        // Save user
        user = userRepository.save(user);
        
        try {
            // Generate self-signed certificate
            Certificate certificate = pkiService.generateSelfSignedCertificate(user);
            certificateRepository.save(certificate);
        } catch (Exception e) {
            throw new CryptoException("Failed to generate certificate for user", e);
        }
        
        return user;
    }
    
    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public List<UserDTO> searchUsers(String query) {
        return userRepository.searchUsers(query).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<UserDTO> getAllActiveUsers() {
        return userRepository.findAllActiveUsers().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPublicKeyRSA(user.getPublicKeyRSA());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setActive(user.isActive());
        return dto;
    }
}
