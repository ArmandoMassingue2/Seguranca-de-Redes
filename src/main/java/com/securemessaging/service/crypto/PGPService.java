package com.securemessaging.service.crypto;

import com.securemessaging.exception.CryptoException;
import com.securemessaging.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

@Service
public class PGPService {
    
    @Autowired
    private RSAKeyService rsaKeyService;
    
    @Autowired
    private HashService hashService;
    
    public PGPEncryptedMessage encrypt(byte[] message, PublicKey recipientPublicKey, 
                                     PrivateKey senderPrivateKey) throws CryptoException {
        try {
            // 1. Generate AES session key
            SecretKey sessionKey = CryptoUtils.generateAESKey();
            
            // 2. Encrypt message with AES
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            byte[] encryptedMessage = aesCipher.doFinal(message);
            
            // 3. Encrypt session key with recipient's RSA public key
            byte[] encryptedSessionKey = rsaKeyService.encrypt(sessionKey.getEncoded(), recipientPublicKey);
            
            // 4. Calculate message hash using both SHA-256 and SHA3-256 for integrity
            byte[] messageHashSHA256 = hashService.sha256(message);
            byte[] messageHashSHA3_256 = hashService.sha3_256(message);
            
            // Combine hashes for enhanced integrity
            byte[] combinedHash = new byte[messageHashSHA256.length + messageHashSHA3_256.length];
            System.arraycopy(messageHashSHA256, 0, combinedHash, 0, messageHashSHA256.length);
            System.arraycopy(messageHashSHA3_256, 0, combinedHash, messageHashSHA256.length, messageHashSHA3_256.length);
            
            // 5. Sign combined hash with sender's private key
            byte[] signature = rsaKeyService.sign(combinedHash, senderPrivateKey);
            
            return new PGPEncryptedMessage(encryptedMessage, encryptedSessionKey, signature, combinedHash);
            
        } catch (Exception e) {
            throw new CryptoException("PGP encryption failed", e);
        }
    }
    
    public byte[] decrypt(PGPEncryptedMessage pgpMessage, PrivateKey recipientPrivateKey, 
                         PublicKey senderPublicKey) throws CryptoException {
        try {
            // 1. Decrypt session key with recipient's RSA private key
            byte[] sessionKeyBytes = rsaKeyService.decrypt(pgpMessage.getEncryptedSessionKey(), recipientPrivateKey);
            SecretKey sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");
            
            // 2. Decrypt message with AES session key
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);
            byte[] decryptedMessage = aesCipher.doFinal(pgpMessage.getEncryptedMessage());
            
            // 3. Verify signature
            boolean signatureValid = rsaKeyService.verify(pgpMessage.getMessageHash(), 
                                                         pgpMessage.getSignature(), senderPublicKey);
            
            if (!signatureValid) {
                throw new CryptoException("Invalid digital signature");
            }
            
            // 4. Verify message integrity with both hash algorithms
            byte[] calculatedHashSHA256 = hashService.sha256(decryptedMessage);
            byte[] calculatedHashSHA3_256 = hashService.sha3_256(decryptedMessage);
            
            // Reconstruct combined hash
            byte[] calculatedCombinedHash = new byte[calculatedHashSHA256.length + calculatedHashSHA3_256.length];
            System.arraycopy(calculatedHashSHA256, 0, calculatedCombinedHash, 0, calculatedHashSHA256.length);
            System.arraycopy(calculatedHashSHA3_256, 0, calculatedCombinedHash, calculatedHashSHA256.length, calculatedHashSHA3_256.length);
            
            boolean integrityValid = Arrays.equals(calculatedCombinedHash, pgpMessage.getMessageHash());
            
            if (!integrityValid) {
                throw new CryptoException("Message integrity check failed");
            }
            
            return decryptedMessage;
            
        } catch (Exception e) {
            throw new CryptoException("PGP decryption failed", e);
        }
    }
    
    public static class PGPEncryptedMessage {
        private final byte[] encryptedMessage;
        private final byte[] encryptedSessionKey;
        private final byte[] signature;
        private final byte[] messageHash;
        
        public PGPEncryptedMessage(byte[] encryptedMessage, byte[] encryptedSessionKey, 
                                  byte[] signature, byte[] messageHash) {
            this.encryptedMessage = encryptedMessage;
            this.encryptedSessionKey = encryptedSessionKey;
            this.signature = signature;
            this.messageHash = messageHash;
        }
        
        // Getters
        public byte[] getEncryptedMessage() { return encryptedMessage; }
        public byte[] getEncryptedSessionKey() { return encryptedSessionKey; }
        public byte[] getSignature() { return signature; }
        public byte[] getMessageHash() { return messageHash; }
    }
}