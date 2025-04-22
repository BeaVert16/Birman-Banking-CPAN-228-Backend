package com.birmanBank.BirmanBankBackend.utils;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.birmanBank.BirmanBankBackend.repositories.ClientRepository;

public class ValidationUtil {

    // validates that a string is not null or empty
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required and cannot be empty");
        }
    }

    public static void validateCardNumber(String cardNumber) {
        // validate card number is not null or empty
        validateNotEmpty(cardNumber, "Card Number");

        // validate card number length
        if (cardNumber.length() != 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card number must be 16 digits long");
        }

        // validate card number contains only digits
        if (!cardNumber.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card number must contain only numeric digits");
        }
    }

    public static void validatePhoneNumber(String phoneNumber) {
        // Validate phone number is not null or empty
        validateNotEmpty(phoneNumber, "Phone Number");

        // Validate phone number length (e.g., 10 digits for local numbers)
        if (phoneNumber.length() != 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number must be 10 digits long");
        }

        // Validate phone number contains only digits
        if (!phoneNumber.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number must contain only numeric digits");
        }
    }

    public static void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be greater than zero");
        }
    }

    public static void validateAccountOwnership(String accountClientId, String expectedClientId) {
        if (!accountClientId.equals(expectedClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account does not belong to the authenticated user");
        }
    }

    public static void validateSufficientBalance(BigDecimal balance, BigDecimal amountNeeded) {
        if (balance.compareTo(amountNeeded) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance in account");
        }
    }

    public static void validateAccountType(String accountType) {
        if (!"Chequing".equalsIgnoreCase(accountType) && !"Savings".equalsIgnoreCase(accountType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account type");
        }
    }

    public static void validateUniquePhoneNumber(String phoneNumber, ClientRepository clientRepository) {
        if (clientRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number already in use");
        }
    }

    public static void validateNonAdminTransfer(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transfers to admin accounts are not allowed");
        }
    }

    public static void validateToken(String token, JwtUtil jwtUtil) {
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
