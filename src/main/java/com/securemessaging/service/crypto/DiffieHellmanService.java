package com.securemessaging.service.crypto;

import com.securemessaging.exception.KeyExchangeException;
import com.securemessaging.util.PRNGUtils;
import org.springframework.stereotype.Service;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;

@Service
public class DiffieHellmanService {
    
    // RFC 2409 - 1024-bit MODP Group conforme requisito do docente
    private static final BigInteger DH_P = new BigInteger(
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74" +
        "020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F1437" +
        "4FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
        "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF", 16);
    
    private static final BigInteger DH_G = BigInteger.valueOf(2);
    private static final int DH_L = 1024; // Tamanho da chave privada
    
    // IMPLEMENTAÇÃO: Acordo de chaves Diffie-Hellman usando PRNG 128-bit
    public DHKeyExchangeResult performDHKeyExchange() throws KeyExchangeException {
        try {
            // Gerar valor privado usando PRNG de 128 bits conforme requisitos
            BigInteger privateValueA = PRNGUtils.generate128BitRandom();
            BigInteger privateValueB = PRNGUtils.generate128BitRandom();
            
            // Calcular valores públicos
            BigInteger publicValueA = DH_G.modPow(privateValueA, DH_P);
            BigInteger publicValueB = DH_G.modPow(privateValueB, DH_P);
            
            // Calcular segredo compartilhado
            BigInteger sharedSecretA = publicValueB.modPow(privateValueA, DH_P);
            BigInteger sharedSecretB = publicValueA.modPow(privateValueB, DH_P);
            
            // Verificar se os segredos são iguais
            if (!sharedSecretA.equals(sharedSecretB)) {
                throw new KeyExchangeException("DH key exchange failed: shared secrets don't match");
            }
            
            // Converter para chave AES
            byte[] sharedSecretBytes = sharedSecretA.toByteArray();
            byte[] aesKeyBytes = new byte[32]; // 256 bits para AES
            
            // Usar os primeiros 32 bytes ou preencher com zeros se necessário
            int copyLength = Math.min(32, sharedSecretBytes.length);
            System.arraycopy(sharedSecretBytes, 0, aesKeyBytes, 0, copyLength);
            
            SecretKey sharedSecretKey = new SecretKeySpec(aesKeyBytes, "AES");
            
            return new DHKeyExchangeResult(
                privateValueA, publicValueA,
                privateValueB, publicValueB,
                sharedSecretKey
            );
            
        } catch (Exception e) {
            throw new KeyExchangeException("Failed to perform DH key exchange", e);
        }
    }
    
    public KeyPair generateDHKeyPair() throws KeyExchangeException {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            DHParameterSpec dhParams = new DHParameterSpec(DH_P, DH_G, DH_L);
            keyPairGen.initialize(dhParams);
            return keyPairGen.generateKeyPair();
        } catch (Exception e) {
            throw new KeyExchangeException("Failed to generate DH key pair", e);
        }
    }
    
    public SecretKey generateSharedSecret(PrivateKey privateKey, PublicKey otherPublicKey) 
            throws KeyExchangeException {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(otherPublicKey, true);
            
            byte[] sharedSecret = keyAgreement.generateSecret();
            
            // Convert to AES key (use first 256 bits)
            byte[] aesKeyBytes = new byte[32]; // 256 bits
            System.arraycopy(sharedSecret, 0, aesKeyBytes, 0, Math.min(32, sharedSecret.length));
            
            return new SecretKeySpec(aesKeyBytes, "AES");
        } catch (Exception e) {
            throw new KeyExchangeException("Failed to generate shared secret", e);
        }
    }
    
    // IMPLEMENTAÇÃO: Simulação manual do DH usando PRNG 128-bit
    public DHSimulationResult simulateDHExchange(String userA, String userB) throws KeyExchangeException {
        try {
            // Gerar valores privados usando PRNG de 128 bits
            BigInteger privateA = PRNGUtils.generate128BitRandom();
            BigInteger privateB = PRNGUtils.generate128BitRandom();
            
            // Calcular valores públicos
            BigInteger publicA = DH_G.modPow(privateA, DH_P);
            BigInteger publicB = DH_G.modPow(privateB, DH_P);
            
            // Simular troca de chaves públicas
            BigInteger sharedSecretA = publicB.modPow(privateA, DH_P);
            BigInteger sharedSecretB = publicA.modPow(privateB, DH_P);
            
            // Verificar se os segredos compartilhados são iguais
            if (!sharedSecretA.equals(sharedSecretB)) {
                throw new KeyExchangeException("DH simulation failed: shared secrets don't match");
            }
            
            // Criar chaves AES a partir do segredo compartilhado
            byte[] secretBytes = sharedSecretA.toByteArray();
            byte[] aesKey = new byte[32];
            System.arraycopy(secretBytes, 0, aesKey, 0, Math.min(32, secretBytes.length));
            
            SecretKey sessionKey = new SecretKeySpec(aesKey, "AES");
            
            return new DHSimulationResult(
                userA, userB,
                privateA, publicA,
                privateB, publicB,
                sharedSecretA,
                sessionKey
            );
            
        } catch (Exception e) {
            throw new KeyExchangeException("DH simulation failed", e);
        }
    }
    
    // Classes auxiliares para resultados
    public static class DHKeyExchangeResult {
        private final BigInteger privateValueA;
        private final BigInteger publicValueA;
        private final BigInteger privateValueB;
        private final BigInteger publicValueB;
        private final SecretKey sharedSecret;
        
        public DHKeyExchangeResult(BigInteger privateValueA, BigInteger publicValueA,
                                  BigInteger privateValueB, BigInteger publicValueB,
                                  SecretKey sharedSecret) {
            this.privateValueA = privateValueA;
            this.publicValueA = publicValueA;
            this.privateValueB = privateValueB;
            this.publicValueB = publicValueB;
            this.sharedSecret = sharedSecret;
        }
        
        // Getters
        public BigInteger getPrivateValueA() { return privateValueA; }
        public BigInteger getPublicValueA() { return publicValueA; }
        public BigInteger getPrivateValueB() { return privateValueB; }
        public BigInteger getPublicValueB() { return publicValueB; }
        public SecretKey getSharedSecret() { return sharedSecret; }
    }
    
    public static class DHSimulationResult {
        private final String userA;
        private final String userB;
        private final BigInteger privateKeyA;
        private final BigInteger publicKeyA;
        private final BigInteger privateKeyB;
        private final BigInteger publicKeyB;
        private final BigInteger sharedSecret;
        private final SecretKey sessionKey;
        
        public DHSimulationResult(String userA, String userB,
                                 BigInteger privateKeyA, BigInteger publicKeyA,
                                 BigInteger privateKeyB, BigInteger publicKeyB,
                                 BigInteger sharedSecret, SecretKey sessionKey) {
            this.userA = userA;
            this.userB = userB;
            this.privateKeyA = privateKeyA;
            this.publicKeyA = publicKeyA;
            this.privateKeyB = privateKeyB;
            this.publicKeyB = publicKeyB;
            this.sharedSecret = sharedSecret;
            this.sessionKey = sessionKey;
        }
        
        // Getters
        public String getUserA() { return userA; }
        public String getUserB() { return userB; }
        public BigInteger getPrivateKeyA() { return privateKeyA; }
        public BigInteger getPublicKeyA() { return publicKeyA; }
        public BigInteger getPrivateKeyB() { return privateKeyB; }
        public BigInteger getPublicKeyB() { return publicKeyB; }
        public BigInteger getSharedSecret() { return sharedSecret; }
        public SecretKey getSessionKey() { return sessionKey; }
    }
}