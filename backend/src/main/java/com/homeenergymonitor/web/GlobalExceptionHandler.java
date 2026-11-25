package com.homeenergymonitor.web;

import com.homeenergymonitor.meter.MeterSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException error) {
        String message = error.getMessage() == null ? "Invalid request" : error.getMessage();
        return ResponseEntity.badRequest().body(body(message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException error) {
        String message = "Invalid value for parameter '" + error.getName() + "'";
        return ResponseEntity.badRequest().body(body(message));
    }

    private static Map<String, Object> body(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "BAD_REQUEST");
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", message);
        body.put("source", MeterSource.SIMULATED.name());
        body.put("demo", true);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
