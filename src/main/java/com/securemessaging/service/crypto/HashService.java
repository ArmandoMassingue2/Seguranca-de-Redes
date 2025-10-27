package com.securemessaging.service.crypto;

import com.securemessaging.exception.CryptoException;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Service
public class HashService {
    
    static {
        // Adicionar BouncyCastle provider para SHA3-256 se não estiver disponível
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    public byte[] sha256(byte[] data) throws CryptoException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("SHA-256 algorithm not available", e);
        }
    }
    
    // CORREÇÃO: Implementar SHA3-256 conforme requisitos
    public byte[] sha3_256(byte[] data) throws CryptoException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            // Tentar com BouncyCastle provider
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA3-256", "BC");
                return digest.digest(data);
            } catch (Exception ex) {
                throw new CryptoException("SHA3-256 algorithm not available", ex);
            }
        }
    }
    
    public byte[] sha3_512(byte[] data) throws CryptoException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-512");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA3-512", "BC");
                return digest.digest(data);
            } catch (Exception ex) {
                throw new CryptoException("SHA3-512 algorithm not available", ex);
            }
        }
    }
    
    // Método para converter dados para hex usando SHA-256
    public String sha256Hex(byte[] data) throws CryptoException {
        return bytesToHex(sha256(data));
    }
    
    // CORREÇÃO: Método separado para converter hash já calculado para hex
    public String hashToHex(byte[] hash) {
        return bytesToHex(hash);
    }
    
    public String sha3_256Hex(byte[] data) throws CryptoException {
        return bytesToHex(sha3_256(data));
    }
    
    public String sha3_512Hex(byte[] data) throws CryptoException {
        return bytesToHex(sha3_512(data));
    }
    
    public boolean verifyHash(byte[] data, byte[] expectedHash, String algorithm) throws CryptoException {
        byte[] actualHash;
        switch (algorithm.toUpperCase()) {
            case "SHA-256":
                actualHash = sha256(data);
                break;
            case "SHA3-256":
                actualHash = sha3_256(data);
                break;
            case "SHA3-512":
                actualHash = sha3_512(data);
                break;
            default:
                throw new CryptoException("Unsupported hash algorithm: " + algorithm);
        }
        return MessageDigest.isEqual(actualHash, expectedHash);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}