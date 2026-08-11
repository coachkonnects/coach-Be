package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email").trim().toLowerCase();
            authService.requestOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP generated and sent to email successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email").trim().toLowerCase();
            String code = body.get("code").trim();
            
            User user = authService.verifyOtp(email, code);
            
            // Note: Returning a simple success message for now. Real JWT generation can be added here.
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "user", user,
                    "session_token", "java-spring-mock-token-12345"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
