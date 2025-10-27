package com.securemessaging.controller;

import com.securemessaging.exception.CryptoException;
import com.securemessaging.model.entity.User;
import com.securemessaging.service.UserService;
import com.securemessaging.service.crypto.PGPService;
import com.securemessaging.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PGPService pgpService;
    
    // Storage simples em memória para arquivos criptografados
    private final Map<String, EncryptedFile> encryptedFiles = new ConcurrentHashMap<>();
    
    // Lista de tipos de arquivo permitidos
    private final List<String> allowedMimeTypes = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
        "audio/mp3", "audio/wav", "audio/ogg", "audio/m4a", "audio/mpeg",
        "application/pdf", "text/plain", "application/zip",
        "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                       @RequestParam("recipient") String recipientUsername,
                                       Principal principal) {
        try {
            System.out.println("=== UPLOAD DE ARQUIVO ===");
            System.out.println("Arquivo: " + file.getOriginalFilename());
            System.out.println("Tamanho: " + file.getSize() + " bytes");
            System.out.println("Tipo: " + file.getContentType());
            System.out.println("De: " + principal.getName() + " Para: " + recipientUsername);
            
            // Validar tamanho do arquivo (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                System.err.println("Arquivo muito grande: " + file.getSize() + " bytes");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File size exceeds 5MB limit"
                ));
            }
            
            // Validar se o arquivo não está vazio
            if (file.isEmpty()) {
                System.err.println("Arquivo vazio");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File is empty"
                ));
            }
            
            // Validar tipo de arquivo
            String contentType = file.getContentType();
            if (contentType == null || !allowedMimeTypes.contains(contentType)) {
                System.err.println("Tipo de arquivo não permitido: " + contentType);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File type not allowed: " + contentType
                ));
            }
            
            // Obter usuários
            User sender = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + principal.getName()));
            User recipient = userService.findByUsername(recipientUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + recipientUsername));
            
            System.out.println("Usuários encontrados - Sender: " + sender.getUsername() + ", Recipient: " + recipient.getUsername());
            
            // Verificar se as chaves RSA existem
            if (sender.getPublicKeyRSA() == null || sender.getPublicKeyRSA().trim().isEmpty()) {
                System.err.println("Sender não tem chave pública RSA");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Sender missing RSA public key"
                ));
            }
            
            if (sender.getPrivateKeyRSA() == null || sender.getPrivateKeyRSA().trim().isEmpty()) {
                System.err.println("Sender não tem chave privada RSA");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Sender missing RSA private key"
                ));
            }
            
            if (recipient.getPublicKeyRSA() == null || recipient.getPublicKeyRSA().trim().isEmpty()) {
                System.err.println("Recipient não tem chave pública RSA");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Recipient missing RSA public key"
                ));
            }
            
            // Obter chaves
            PublicKey recipientPublicKey;
            PrivateKey senderPrivateKey;
            
            try {
                recipientPublicKey = CryptoUtils.getPublicKeyFromString(recipient.getPublicKeyRSA());
                senderPrivateKey = CryptoUtils.getPrivateKeyFromString(sender.getPrivateKeyRSA());
                System.out.println("Chaves RSA carregadas com sucesso");
            } catch (Exception e) {
                System.err.println("Erro ao carregar chaves RSA: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to load RSA keys: " + e.getMessage()
                ));
            }
            
            // Ler conteúdo do arquivo
            byte[] fileContent;
            try {
                fileContent = file.getBytes();
                System.out.println("Conteúdo do arquivo lido: " + fileContent.length + " bytes");
            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo: " + e.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to read file: " + e.getMessage()
                ));
            }
            
            // Criptografar arquivo usando PGP
            PGPService.PGPEncryptedMessage encryptedMessage;
            try {
                System.out.println("Iniciando criptografia PGP...");
                encryptedMessage = pgpService.encrypt(fileContent, recipientPublicKey, senderPrivateKey);
                System.out.println("Arquivo criptografado com sucesso");
                System.out.println("Tamanho criptografado: " + encryptedMessage.getEncryptedMessage().length + " bytes");
            } catch (CryptoException e) {
                System.err.println("Erro na criptografia: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Encryption failed: " + e.getMessage()
                ));
            }
            
            // Gerar ID único para o arquivo
            String fileId = UUID.randomUUID().toString();
            System.out.println("ID do arquivo gerado: " + fileId);
            
            // Armazenar arquivo criptografado
            EncryptedFile encryptedFile = new EncryptedFile(
                fileId,
                file.getOriginalFilename(),
                contentType,
                file.getSize(),
                sender.getUsername(),
                recipientUsername,
                encryptedMessage
            );
            
            encryptedFiles.put(fileId, encryptedFile);
            System.out.println("Arquivo armazenado com sucesso. Total de arquivos: " + encryptedFiles.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "fileId", fileId,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize(),
                "contentType", contentType,
                "message", "File encrypted and uploaded successfully"
            ));
            
        } catch (Exception e) {
            System.err.println("Erro geral no upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Upload failed: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId, Principal principal) {
        try {
            System.out.println("=== DOWNLOAD DE ARQUIVO ===");
            System.out.println("FileID: " + fileId);
            System.out.println("Usuário: " + principal.getName());
            
            EncryptedFile encryptedFile = encryptedFiles.get(fileId);
            if (encryptedFile == null) {
                System.err.println("Arquivo não encontrado: " + fileId);
                System.err.println("Arquivos disponíveis: " + encryptedFiles.keySet());
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("Arquivo encontrado: " + encryptedFile.getFileName());
            
            // Verificar se o usuário tem permissão para baixar o arquivo
            String username = principal.getName();
            if (!encryptedFile.getSenderUsername().equals(username) && 
                !encryptedFile.getRecipientUsername().equals(username)) {
                System.err.println("Acesso negado para usuário " + username + " ao arquivo " + fileId);
                System.err.println("Sender: " + encryptedFile.getSenderUsername() + ", Recipient: " + encryptedFile.getRecipientUsername());
                return ResponseEntity.status(403).build();
            }
            
            System.out.println("Permissão verificada para usuário: " + username);
            
            // Obter chaves para descriptografia
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            
            if (user.getPrivateKeyRSA() == null || user.getPrivateKeyRSA().trim().isEmpty()) {
                System.err.println("Usuário " + username + " não tem chave privada RSA");
                return ResponseEntity.badRequest().build();
            }
            
            PrivateKey userPrivateKey;
            try {
                userPrivateKey = CryptoUtils.getPrivateKeyFromString(user.getPrivateKeyRSA());
                System.out.println("Chave privada do usuário carregada");
            } catch (Exception e) {
                System.err.println("Erro ao carregar chave privada: " + e.getMessage());
                return ResponseEntity.badRequest().build();
            }
            
            // Determinar o remetente para verificar assinatura
            String senderUsername = encryptedFile.getSenderUsername();
            User sender = userService.findByUsername(senderUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderUsername));
            
            if (sender.getPublicKeyRSA() == null || sender.getPublicKeyRSA().trim().isEmpty()) {
                System.err.println("Sender " + senderUsername + " não tem chave pública RSA");
                return ResponseEntity.badRequest().build();
            }
            
            PublicKey senderPublicKey;
            try {
                senderPublicKey = CryptoUtils.getPublicKeyFromString(sender.getPublicKeyRSA());
                System.out.println("Chave pública do sender carregada");
            } catch (Exception e) {
                System.err.println("Erro ao carregar chave pública do sender: " + e.getMessage());
                return ResponseEntity.badRequest().build();
            }
            
            // Descriptografar arquivo
            byte[] decryptedContent;
            try {
                System.out.println("Iniciando descriptografia...");
                decryptedContent = pgpService.decrypt(
                    encryptedFile.getEncryptedMessage(), userPrivateKey, senderPublicKey);
                System.out.println("Arquivo descriptografado com sucesso!");
                System.out.println("Tamanho descriptografado: " + decryptedContent.length + " bytes");
            } catch (CryptoException e) {
                System.err.println("Erro na descriptografia: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(500).build();
            }
            
            ByteArrayResource resource = new ByteArrayResource(decryptedContent);
            
            System.out.println("Preparando resposta para download...");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + encryptedFile.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(encryptedFile.getContentType()))
                    .contentLength(decryptedContent.length)
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("Erro geral no download do arquivo " + fileId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/info/{fileId}")
    public ResponseEntity<?> getFileInfo(@PathVariable String fileId, Principal principal) {
        System.out.println("Solicitando info do arquivo: " + fileId + " para usuário: " + principal.getName());
        
        EncryptedFile encryptedFile = encryptedFiles.get(fileId);
        if (encryptedFile == null) {
            System.err.println("Arquivo não encontrado para info: " + fileId);
            return ResponseEntity.notFound().build();
        }
        
        // Verificar permissões
        String username = principal.getName();
        if (!encryptedFile.getSenderUsername().equals(username) && 
            !encryptedFile.getRecipientUsername().equals(username)) {
            System.err.println("Acesso negado para info do arquivo");
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(Map.of(
            "fileId", encryptedFile.getFileId(),
            "fileName", encryptedFile.getFileName(),
            "contentType", encryptedFile.getContentType(),
            "fileSize", encryptedFile.getFileSize(),
            "sender", encryptedFile.getSenderUsername(),
            "recipient", encryptedFile.getRecipientUsername(),
            "encrypted", true
        ));
    }
    
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<Resource> previewFile(@PathVariable String fileId, Principal principal) {
        try {
            System.out.println("Preview solicitado para arquivo: " + fileId);
            
            EncryptedFile encryptedFile = encryptedFiles.get(fileId);
            if (encryptedFile == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verificar se é uma imagem
            if (!encryptedFile.getContentType().startsWith("image/")) {
                System.err.println("Arquivo não é imagem: " + encryptedFile.getContentType());
                return ResponseEntity.badRequest().build();
            }
            
            // Verificar permissões
            String username = principal.getName();
            if (!encryptedFile.getSenderUsername().equals(username) && 
                !encryptedFile.getRecipientUsername().equals(username)) {
                return ResponseEntity.status(403).build();
            }
            
            // Descriptografar para preview
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            PrivateKey userPrivateKey = CryptoUtils.getPrivateKeyFromString(user.getPrivateKeyRSA());
            
            User sender = userService.findByUsername(encryptedFile.getSenderUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
            PublicKey senderPublicKey = CryptoUtils.getPublicKeyFromString(sender.getPublicKeyRSA());
            
            byte[] decryptedContent = pgpService.decrypt(
                encryptedFile.getEncryptedMessage(), userPrivateKey, senderPublicKey);
            
            ByteArrayResource resource = new ByteArrayResource(decryptedContent);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(encryptedFile.getContentType()))
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("Erro no preview: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId, Principal principal) {
        System.out.println("Tentativa de deletar arquivo: " + fileId + " por usuário: " + principal.getName());
        
        EncryptedFile encryptedFile = encryptedFiles.get(fileId);
        if (encryptedFile == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Apenas o remetente pode excluir o arquivo
        if (!encryptedFile.getSenderUsername().equals(principal.getName())) {
            System.err.println("Apenas o sender pode deletar o arquivo");
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "error", "Only sender can delete the file"
            ));
        }
        
        encryptedFiles.remove(fileId);
        System.out.println("Arquivo deletado com sucesso. Arquivos restantes: " + encryptedFiles.size());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "File deleted successfully"
        ));
    }
    
    // Endpoint para listar todos os arquivos (debug)
    @GetMapping("/list")
    public ResponseEntity<?> listFiles(Principal principal) {
        System.out.println("Listando arquivos para usuário: " + principal.getName());
        System.out.println("Total de arquivos no sistema: " + encryptedFiles.size());
        
        return ResponseEntity.ok(Map.of(
            "totalFiles", encryptedFiles.size(),
            "files", encryptedFiles.keySet()
        ));
    }
    
    // Classe interna para armazenar arquivos criptografados
    private static class EncryptedFile {
        private final String fileId;
        private final String fileName;
        private final String contentType;
        private final long fileSize;
        private final String senderUsername;
        private final String recipientUsername;
        private final PGPService.PGPEncryptedMessage encryptedMessage;
        
        public EncryptedFile(String fileId, String fileName, String contentType, long fileSize,
                           String senderUsername, String recipientUsername,
                           PGPService.PGPEncryptedMessage encryptedMessage) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.senderUsername = senderUsername;
            this.recipientUsername = recipientUsername;
            this.encryptedMessage = encryptedMessage;
        }
        
        // Getters
        public String getFileId() { return fileId; }
        public String getFileName() { return fileName; }
        public String getContentType() { return contentType; }
        public long getFileSize() { return fileSize; }
        public String getSenderUsername() { return senderUsername; }
        public String getRecipientUsername() { return recipientUsername; }
        public PGPService.PGPEncryptedMessage getEncryptedMessage() { return encryptedMessage; }
    }
}