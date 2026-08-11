package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/super-admins")
public class SuperAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping
    public ResponseEntity<?> getAllAdmins() {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .toList();
        return ResponseEntity.ok(admins);
    }

    @PostMapping
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered.");
        }

        User newAdmin = new User();
        newAdmin.setEmail(email);
        newAdmin.setRole("ADMIN");
        newAdmin.setPasswordHash("TempPassword123!");

        User savedAdmin = userRepository.save(newAdmin);
        
        sendWelcomeEmail(email, "TempPassword123!");
        
        return ResponseEntity.ok(Map.of(
                "message", "Super Admin created successfully. An email has been sent to them with their temporary password.",
                "admin", savedAdmin
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        User admin = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if ("admin@coachkonnects.com".equals(admin.getEmail())) {
            return ResponseEntity.badRequest().body("Cannot delete the root admin account.");
        }
        
        userRepository.delete(admin);
        return ResponseEntity.ok(Map.of("message", "Admin access revoked successfully."));
    }

    private void sendWelcomeEmail(String to, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("coachkonnects@gmail.com", "CoachKonnects Security");
            helper.setTo(to);
            helper.setSubject("You've been invited as a Super Admin!");

            String htmlMsg = "<div style=\"font-family: Arial; padding: 20px; border: 1px solid #e0e0e0;\">"
                    + "<h2 style=\"color: #f26b21;\">Welcome to CoachKonnects!</h2>"
                    + "<p>You have been invited to manage the CoachKonnects platform as a Super Admin.</p>"
                    + "<p>Your temporary password is:</p>"
                    + "<div style=\"background-color: #fff2e8; padding: 15px; font-size: 24px; font-weight: bold;\">"
                    + tempPassword + "</div>"
                    + "<p>Please log in at the admin portal and change your password immediately.</p></div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
