package com.sgitu.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Service for managing email verification codes.
 * Uses Redis for temporary storage with automatic expiration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String VERIFICATION_CODE_PREFIX = "email_verification:";
    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a 6-digit verification code.
     * @return Random 6-digit code (100000-999999)
     */
    public String generateVerificationCode() {
        int code = RANDOM.nextInt(900000) + 100000; // Range: 100000 to 999999
        return String.valueOf(code);
    }

    /**
     * Stores verification code in Redis with TTL.
     * Key format: email_verification:{email} -> code
     * 
     * @param email User's email
     * @param code Verification code
     */
    public void storeVerificationCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION);
        log.info("Verification code stored for {} (expires in 15 minutes)", email);
    }

    /**
     * Verifies if the provided code matches the stored code.
     * Deletes the code after successful verification.
     * 
     * @param email User's email
     * @param providedCode Code provided by user
     * @return true if code is valid, false otherwise
     */
    public boolean verifyCode(String email, String providedCode) {
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("No verification code found or expired for: {}", email);
            return false;
        }
        
        boolean isValid = storedCode.equals(providedCode);
        
        if (isValid) {
            // Delete code after successful verification (one-time use)
            redisTemplate.delete(key);
            log.info("Verification code successfully validated for: {}", email);
        } else {
            log.warn("Invalid verification code provided for: {}", email);
        }
        
        return isValid;
    }

    /**
     * Deletes verification code from Redis.
     * 
     * @param email User's email
     */
    public void deleteVerificationCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Verification code deleted for: {}", email);
    }

    /**
     * Checks if an active verification code exists for the email.
     * 
     * @param email User's email
     * @return true if code exists, false otherwise
     */
    public boolean hasActiveCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Gets remaining TTL for verification code (in seconds).
     * 
     * @param email User's email
     * @return TTL in seconds, or null if key doesn't exist
     */
    public Long getCodeTTL(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return redisTemplate.getExpire(key);
    }
}
