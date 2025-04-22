package com.birmanBank.BirmanBankBackend.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        // Prepare a map to send the message
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getReason());
        // Return the response with status code and message
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }
}
