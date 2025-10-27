package com.securemessaging.exception;

public class CertificateException extends Exception {
    public CertificateException(String message) {
        super(message);
    }
    
    public CertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}