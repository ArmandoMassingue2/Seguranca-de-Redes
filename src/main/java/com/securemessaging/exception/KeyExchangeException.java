package com.securemessaging.exception;

public class KeyExchangeException extends Exception {
    public KeyExchangeException(String message) {
        super(message);
    }
    
    public KeyExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}