package com.securemessaging.util;

import java.security.SecureRandom;
import java.math.BigInteger;

public class PRNGUtils {
    private static final SecureRandom secureRandom = new SecureRandom();
    
    public static BigInteger generate128BitRandom() {
        byte[] randomBytes = new byte[16]; // 128 bits = 16 bytes
        secureRandom.nextBytes(randomBytes);
        return new BigInteger(1, randomBytes);
    }
    
    public static BigInteger generateRandomInRange(BigInteger min, BigInteger max) {
        BigInteger range = max.subtract(min);
        int bitLength = range.bitLength();
        BigInteger random;
        do {
            random = new BigInteger(bitLength, secureRandom);
        } while (random.compareTo(range) >= 0);
        return random.add(min);
    }
}
