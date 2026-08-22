package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.Enquiry;
import com.coachkonnects.backend.model.EnquiryStatus;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.EnquiryRepository;
import com.coachkonnects.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import com.coachkonnects.backend.util.JwtUtil;

import java.util.Map;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.coachkonnects.backend.repository.StudentProfileRepository studentProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:https://coachkonnects.com}")
    private String frontendUrl;
    @PostMapping("/send")
    public ResponseEntity<?> sendEnquiry(@RequestBody Map<String, String> payload) {
        try {
            String leadEmail   = payload.get("email");
            String coachSlug   = payload.get("coachSlug");
            String message     = payload.get("message");
            String leadName    = payload.get("name");
            String rawPhone    = payload.get("phone");
            String leadLocation = payload.get("location");

            if (leadEmail == null || coachSlug == null || message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields."));
            }

            // If name or phone is empty, try to fetch from registered user profile
            if ((leadName == null || leadName.trim().isEmpty()) || (rawPhone == null || rawPhone.trim().isEmpty())) {
                com.coachkonnects.backend.model.User existingUser = userRepository.findByEmail(leadEmail).orElse(null);
                if (existingUser != null) {
                    if (rawPhone == null || rawPhone.trim().isEmpty()) rawPhone = existingUser.getPhoneNumber();
                    com.coachkonnects.backend.model.StudentProfile sp = studentProfileRepository.findByUser(existingUser).orElse(null);
                    if (sp != null) {
                        if (leadName == null || leadName.trim().isEmpty()) leadName = sp.getFullName();
                    }
                }
            }
            
            leadName = (leadName == null || leadName.trim().isEmpty()) ? "Visitor" : leadName;
            String leadPhone = (rawPhone != null && rawPhone.trim().isEmpty()) ? null : rawPhone;

            String token = null;
            // Auto-register student if they don't exist
            com.coachkonnects.backend.model.User existingUserCheck = userRepository.findByEmail(leadEmail.toLowerCase()).orElse(null);
            if (existingUserCheck == null) {
                if (userRepository.findByPhoneNumber(leadPhone).isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "number already registered try new number"));
                }
                com.coachkonnects.backend.model.User newUser = new com.coachkonnects.backend.model.User();
                newUser.setEmail(leadEmail.toLowerCase());
                newUser.setRole("STUDENT");
                newUser.setPhoneNumber(leadPhone);
                userRepository.save(newUser);
                token = jwtUtil.generateToken(newUser.getEmail(), "STUDENT");
            }



            // Find the target coach (Regardless of PENDING/APPROVED status)
            CoachProfile coach = null;
            try {
                Long id = Long.parseLong(coachSlug);
                coach = coachProfileRepository.findById(id).orElse(null);
            } catch (NumberFormatException e) {
                coach = coachProfileRepository.findBySlug(coachSlug).orElse(null);
            }
            if (coach == null) {
                throw new RuntimeException("Coach not found.");
            }

            // Spam protection logic
            long currentEnquiries = enquiryRepository.countByLeadEmailAndCoach(leadEmail, coach);
            int wordCount = 1;
            if (coach.getExpertise() != null && !coach.getExpertise().trim().isEmpty()) {
                wordCount = coach.getExpertise().trim().split("\\s+").length;
            }
            long maxEnquiries = Math.min(6, Math.max(1, wordCount) * 2);

            if (currentEnquiries >= maxEnquiries) {
                return ResponseEntity.status(429).body(Map.of("error", "You have reached the maximum number of enquiries for this coach based on their expertise. Please wait for their response."));
            }

            // Save the lead directly on the enquiry — no User/StudentProfile creation
            Enquiry enquiry = new Enquiry();
            enquiry.setLeadName(leadName);
            enquiry.setLeadEmail(leadEmail);
            enquiry.setLeadPhone(leadPhone);
            enquiry.setLeadLocation(leadLocation);
            enquiry.setCoach(coach);
            enquiry.setMessage(message);
            enquiry.setStatus(EnquiryStatus.PENDING_COACH_APPROVAL);

            enquiryRepository.save(enquiry);

            if (token != null) {
                return ResponseEntity.ok(Map.of("message", "Enquiry sent successfully!", "token", token));
            }
            return ResponseEntity.ok(Map.of("message", "Enquiry sent successfully! Admin will review it shortly."));
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }
    }

    @GetMapping("/coach")
    public ResponseEntity<?> getCoachEnquiries(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("userEmail");
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            CoachProfile coach = coachProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Coach profile not found."));

            // Coaches should only see leads that have passed Admin Approval
            // e.g., PENDING_COACH_APPROVAL, APPROVED, REJECTED (by coach)
            var allEnquiries = enquiryRepository.findByCoach(coach);
            var filteredEnquiries = allEnquiries.stream()
                    .filter(e -> e.getStatus() != EnquiryStatus.PENDING_ADMIN_APPROVAL)
                    .toList();

            return ResponseEntity.ok(filteredEnquiries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student")
    public ResponseEntity<?> getStudentEnquiries(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("userEmail");
            // Find enquiries directly by email since the student might not have an attached StudentProfile on the Enquiry yet
            var enquiries = enquiryRepository.findByLeadEmail(email);
            return ResponseEntity.ok(enquiries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateEnquiryStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String statusStr = payload.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required."));
            }

            Enquiry enquiry = enquiryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Enquiry not found."));

            EnquiryStatus status = EnquiryStatus.valueOf(statusStr);
            enquiry.setStatus(status);
            enquiryRepository.save(enquiry);

            String responseMessage = "Status updated successfully!";
            if (status == EnquiryStatus.APPROVED) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom("support@coachkonnects.com");
                    message.setTo(enquiry.getLeadEmail());
                    message.setSubject("Good news! Your Coach has accepted your enquiry!");
                    message.setText("Hello " + enquiry.getLeadName() + ",\n\n" +
                            "Great news! " + enquiry.getCoach().getFullName() + " has approved your enquiry on CoachKonnects.\n\n" +
                            "Login here: " + frontendUrl + "/login?type=student\n\n" +
                            "Best regards,\nCoachKonnects Team");
                    mailSender.send(message);
                } catch (Exception e) {
                    System.err.println("Failed to send approval email: " + e.getMessage());
                    responseMessage = "Status updated, but failed to send email to student: " + e.getMessage();
                }
            }

            return ResponseEntity.ok(Map.of("message", responseMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
