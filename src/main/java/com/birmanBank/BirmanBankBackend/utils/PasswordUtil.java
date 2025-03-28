package com.birmanBank.BirmanBankBackend.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Encrypt a password
    public static String encryptPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // Validate a password
    public static boolean validatePassword(String rawPassword, String encryptedPassword) {
        return encoder.matches(rawPassword, encryptedPassword);
    }
}