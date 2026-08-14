package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.Enquiry;
import com.coachkonnects.backend.model.EnquiryStatus;
import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.EnquiryRepository;
import com.coachkonnects.backend.repository.StudentProfileRepository;
import com.coachkonnects.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<?> sendEnquiry(@RequestBody Map<String, String> payload) {
        try {
            String studentEmail = payload.get("email");
            String coachSlug = payload.get("coachSlug");
            String message = payload.get("message");

            if (studentEmail == null || coachSlug == null || message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields."));
            }

            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            StudentProfile student = studentProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Only registered students can send enquiries."));

            CoachProfile coach = coachProfileRepository.findBySlugAndStatusAndIsActiveTrue(coachSlug, com.coachkonnects.backend.model.ProfileStatus.APPROVED)
                    .orElseThrow(() -> new RuntimeException("Coach not found or not approved."));

            Enquiry enquiry = new Enquiry();
            enquiry.setStudent(student);
            enquiry.setCoach(coach);
            enquiry.setMessage(message);
            enquiry.setStatus(EnquiryStatus.PENDING_COACH_APPROVAL);
            
            enquiryRepository.save(enquiry);

            return ResponseEntity.ok(Map.of("message", "Enquiry sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/coach")
    public ResponseEntity<?> getCoachEnquiries(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            CoachProfile coach = coachProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Coach profile not found."));

            return ResponseEntity.ok(enquiryRepository.findByCoach(coach));
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

            return ResponseEntity.ok(Map.of("message", "Status updated successfully!", "enquiry", enquiry));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
