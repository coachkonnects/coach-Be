package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;


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

            if ("ADMIN".equals(user.getRole())) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(user.getEmail());
                    message.setSubject("Security Alert: New Admin Login");
                    message.setText("Hello Admin,\n\nA new login was just detected on your CoachKonnects admin account.\nIf this was you, you can safely ignore this email.\nIf this wasn't you, please secure your account immediately.");
                    mailSender.send(message);
                } catch (Exception ex) {
                    System.err.println("Failed to send admin login alert: " + ex.getMessage());
                }
            }

            
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
