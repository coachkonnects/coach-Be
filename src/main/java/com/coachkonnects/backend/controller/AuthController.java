package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;


import java.util.Map;

import com.coachkonnects.backend.util.JwtUtil;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private JwtUtil jwtUtil;

    // Simple in-memory rate limiting: IP -> number of requests
    private final ConcurrentHashMap<String, Integer> otpRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> otpTimestamps = new ConcurrentHashMap<>();

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        try {
            String ip = request.getRemoteAddr();
            long now = System.currentTimeMillis();
            
            // Reset count if 15 minutes have passed
            if (otpTimestamps.containsKey(ip) && (now - otpTimestamps.get(ip)) > 15 * 60 * 1000) {
                otpRequests.remove(ip);
            }
            
            int attempts = otpRequests.getOrDefault(ip, 0);
            // Bypassed for testing phase
            if (attempts >= 5000) {
                return ResponseEntity.status(429).body(Map.of("error", "Too many requests. Please try again later."));
            }
            
            otpRequests.put(ip, attempts + 1);
            otpTimestamps.put(ip, now);

            String email = body.get("email").trim().toLowerCase();
            String intendedRole = body.get("intendedRole");

            if ("ADMIN".equalsIgnoreCase(intendedRole)) {
                java.util.List<String> allowedAdmins = java.util.Arrays.asList("kavita.ganatra1@gmail.com", "kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                if (!allowedAdmins.contains(email)) {
                    try {
                        org.springframework.mail.SimpleMailMessage alert = new org.springframework.mail.SimpleMailMessage();
                        alert.setFrom("support@coachkonnects.com");
                        alert.setTo("kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                        alert.setSubject("CRITICAL SECURITY ALERT: Unauthorized Admin Login Attempt");
                        alert.setText("Someone with the email address " + email + " just attempted to request an OTP for the Admin Portal.\n\nTheir access was successfully BLOCKED.\n\nPlease review your admin accounts immediately.");
                        mailSender.send(alert);
                    } catch (Exception ex) {}
                    return ResponseEntity.status(401).body(Map.of("error", "Unauthorized: Your email address is not whitelisted for Admin access."));
                }
            }

            authService.requestOtp(email, intendedRole);
            return ResponseEntity.ok(Map.of("message", "OTP generated and sent to email successfully."));
        } catch (Exception e) {
            // Avoid leaking whether the user exists or not
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to process OTP request."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email").trim().toLowerCase();
            String code = body.get("code").trim();
            String intendedRole = body.get("intendedRole");
            
            if ("ADMIN".equalsIgnoreCase(intendedRole)) {
                java.util.List<String> allowedAdmins = java.util.Arrays.asList("kavita.ganatra1@gmail.com", "kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                if (!allowedAdmins.contains(email)) {
                    try {
                        org.springframework.mail.SimpleMailMessage alert = new org.springframework.mail.SimpleMailMessage();
                        alert.setFrom("support@coachkonnects.com");
                        alert.setTo("kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                        alert.setSubject("CRITICAL SECURITY ALERT: Unauthorized Admin Login Attempt");
                        alert.setText("Someone with the email address " + email + " just attempted to VERIFY an OTP for the Admin Portal.\n\nTheir access was successfully BLOCKED.\n\nPlease review your admin accounts immediately.");
                        mailSender.send(alert);
                    } catch (Exception ex) {}
                    return ResponseEntity.status(401).body(Map.of("error", "Unauthorized: Your email address is not whitelisted for Admin access."));
                }
            }

            User user = authService.verifyOtp(email, code);

            if ("ADMIN".equals(user.getRole())) {
                java.util.List<String> allowedAdmins = java.util.Arrays.asList("kavita.ganatra1@gmail.com", "kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                if (!allowedAdmins.contains(email)) {
                    try {
                        org.springframework.mail.SimpleMailMessage alert = new org.springframework.mail.SimpleMailMessage();
                        alert.setFrom("support@coachkonnects.com");
                        alert.setTo("kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                        alert.setSubject("CRITICAL SECURITY ALERT: Unauthorized Admin Login Attempt");
                        alert.setText("Someone with the email address " + email + " just attempted to log into the CoachKonnects Admin Portal.\n\nTheir access was successfully BLOCKED by the security firewall.\n\nPlease review your admin accounts immediately.");
                        mailSender.send(alert);
                    } catch (Exception ex) {
                        System.err.println("Failed to send admin security alert: " + ex.getMessage());
                    }
                    throw new RuntimeException("Unauthorized: Your email address is not whitelisted for Admin access.");
                }

                try {
                    org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
                    message.setFrom("support@coachkonnects.com");
                    message.setTo(user.getEmail());
                    message.setSubject("Security Alert: New Admin Login");
                    message.setText("Hello Admin,\n\nA new login was just detected on your CoachKonnects admin account.\nIf this was you, you can safely ignore this email.\nIf this wasn't you, please secure your account immediately.");
                    mailSender.send(message);
                } catch (Exception ex) {
                    System.err.println("Failed to send admin login alert: " + ex.getMessage());
                }
            }
            
            String roleToIssue = user.getRole();
            if ("ADMIN".equalsIgnoreCase(intendedRole)) {
                java.util.List<String> allowedAdmins = java.util.Arrays.asList("kavita.ganatra1@gmail.com", "kavita.ganatra2@gmail.com", "sameer.rcssoft@gmail.com");
                if (allowedAdmins.contains(email)) {
                    roleToIssue = "ADMIN";
                }
            }
            String token = jwtUtil.generateToken(user.getEmail(), roleToIssue);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "user", user,
                    "session_token", token,
                    "token", token // Providing it as both names for frontend compatibility
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
