package com.github.accessreport.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 401 — bad or expired GitHub token
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(
            HttpClientErrorException e) {
        log.error("GitHub authentication failed: {}", e.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", "GitHub authentication failed");
        body.put("hint", "Check your GITHUB_TOKEN environment variable");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 404 — org name is wrong
    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            HttpClientErrorException e) {
        log.error("Resource not found: {}", e.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", "Organization not found");
        body.put("hint", "Check your GITHUB_ORG environment variable");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 403 — token does not have enough permissions
    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            HttpClientErrorException e) {
        log.error("GitHub access forbidden: {}", e.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", "Access forbidden");
        body.put("hint", "Make sure your token has Members and Administration read permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // 500 — anything else unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        log.error("Unexpected error occurred", e);
        Map<String, String> body = new HashMap<>();
        body.put("error", "Failed to generate access report");
        body.put("details", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}