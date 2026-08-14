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

import java.util.Map;

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
    private EntityManager entityManager;
    @PostMapping("/send")
    public ResponseEntity<?> sendEnquiry(@RequestBody Map<String, String> payload) {
        try {
            String leadEmail   = payload.get("email");
            String coachSlug   = payload.get("coachSlug");
            String message     = payload.get("message");
            String leadName    = payload.getOrDefault("name", "Visitor");
            String rawPhone    = payload.get("phone");
            String leadPhone   = (rawPhone != null && rawPhone.trim().isEmpty()) ? null : rawPhone;
            String leadLocation = payload.get("location");

            if (leadEmail == null || coachSlug == null || message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields."));
            }

            // Find the target coach
            CoachProfile coach = coachProfileRepository
                    .findBySlugAndStatusAndIsActiveTrue(coachSlug, com.coachkonnects.backend.model.ProfileStatus.APPROVED)
                    .orElseThrow(() -> new RuntimeException("Coach not found or not approved."));

            // Save the lead directly on the enquiry — no User/StudentProfile creation
            Enquiry enquiry = new Enquiry();
            enquiry.setLeadName(leadName);
            enquiry.setLeadEmail(leadEmail);
            enquiry.setLeadPhone(leadPhone);
            enquiry.setLeadLocation(leadLocation);
            enquiry.setCoach(coach);
            enquiry.setMessage(message);
            boolean isExistingUser = userRepository.findByEmail(leadEmail).isPresent();
            enquiry.setStatus(isExistingUser ? EnquiryStatus.PENDING_COACH_APPROVAL : EnquiryStatus.PENDING_ADMIN_APPROVAL);

            enquiryRepository.save(enquiry);

            return ResponseEntity.ok(Map.of("message", "Enquiry sent successfully! Admin will review it shortly."));
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }
    }

    @GetMapping("/coach")
    public ResponseEntity<?> getCoachEnquiries(@RequestParam String email) {
        try {
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

            return ResponseEntity.ok(Map.of("message", "Status updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
