package com.securemessaging.service.crypto;

import com.securemessaging.exception.CryptoException;
import com.securemessaging.util.CryptoUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Service
public class RSAKeyService {
    
    public KeyPair generateKeyPair() throws CryptoException {
        try {
            return CryptoUtils.generateRSAKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Failed to generate RSA key pair", e);
        }
    }
    
    public byte[] encrypt(byte[] data, PublicKey publicKey) throws CryptoException {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new CryptoException("RSA encryption failed", e);
        }
    }
    
    public byte[] decrypt(byte[] encryptedData, PrivateKey privateKey) throws CryptoException {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new CryptoException("RSA decryption failed", e);
        }
    }
    
    public byte[] sign(byte[] data, PrivateKey privateKey) throws CryptoException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new CryptoException("Digital signature failed", e);
        }
    }
    
    public boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws CryptoException {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            throw new CryptoException("Signature verification failed", e);
        }
    }
}
