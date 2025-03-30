package com.birmanBank.BirmanBankBackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/*
 * the purpose of this is to generate and validate JWT tokens
 * it uses the secret key from application.properties to sign the tokens
 * the generateToken method creates a token with the username as the subject
 * the validateToken method checks if the token is valid
 * the extractUsername method retrieves the username from the token
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        //convert the secret string to a SecretKey using HMAC SHA algorithm for JWT
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    //generate JWT Token
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // 30 minutes
                .signWith(secretKey)
                .compact();
    }

    //validate JWT Token - checks if the token is valid
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //extract username from JWT Token
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}