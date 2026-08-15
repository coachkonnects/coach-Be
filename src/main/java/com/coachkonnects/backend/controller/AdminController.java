package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.model.Enquiry;
import com.coachkonnects.backend.model.EnquiryStatus;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.StudentProfileRepository;
import com.coachkonnects.backend.repository.EnquiryRepository;
import com.coachkonnects.backend.repository.CoachClassRepository;
import com.coachkonnects.backend.model.CoachClass;
import com.coachkonnects.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private CoachProfileRepository coachRepository;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private CoachClassRepository classRepository;

    @GetMapping("/classes")
    public ResponseEntity<?> getAllClasses() {
        return ResponseEntity.ok(classRepository.findAll().stream().map(c -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("title", c.getTitle());
            map.put("type", c.getType());
            map.put("schedule", c.getSchedule());
            map.put("price", c.getPrice());
            map.put("capacity", c.getCapacity());
            map.put("createdAt", c.getCreatedAt());
            map.put("status", c.getStatus());
            map.put("rejectReason", c.getRejectReason());
            map.put("coachEmail", c.getUser() != null ? c.getUser().getEmail() : "Unknown");
            return map;
        }).toList());
    }

    @PostMapping("/classes/{id}/approve")
    public ResponseEntity<?> approveClass(@PathVariable Long id) {
        CoachClass coachClass = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        coachClass.setStatus(com.coachkonnects.backend.model.ProfileStatus.APPROVED);
        coachClass.setRejectReason(null);
        return ResponseEntity.ok(classRepository.save(coachClass));
    }

    @PostMapping("/classes/{id}/reject")
    public ResponseEntity<?> rejectClass(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        CoachClass coachClass = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        coachClass.setStatus(com.coachkonnects.backend.model.ProfileStatus.REJECTED);
        coachClass.setRejectReason(body.get("reason"));
        return ResponseEntity.ok(classRepository.save(coachClass));
    }

    @DeleteMapping("/classes/{id}")
    public ResponseEntity<?> deleteClassAdmin(@PathVariable Long id) {
        classRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/enquiries")
    public ResponseEntity<?> getAllEnquiries() {
        return ResponseEntity.ok(enquiryRepository.findAll());
    }

    @PutMapping("/enquiries/{id}/approve")
    public ResponseEntity<?> approveEnquiry(@PathVariable Long id) {
        try {
            Enquiry enquiry = enquiryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Enquiry not found"));
            enquiry.setStatus(EnquiryStatus.PENDING_COACH_APPROVAL);
            enquiryRepository.save(enquiry);
            return ResponseEntity.ok(Map.of("message", "Lead approved and sent to coach."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/enquiries/{id}/reject")
    public ResponseEntity<?> rejectEnquiry(@PathVariable Long id) {
        try {
            Enquiry enquiry = enquiryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Enquiry not found"));
            enquiry.setStatus(EnquiryStatus.REJECTED);
            enquiryRepository.save(enquiry);
            return ResponseEntity.ok(Map.of("message", "Lead rejected."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Get all coaches ────────────────────────────────────────────────
    @GetMapping("/coaches")
    public ResponseEntity<?> getAllCoaches() {
        List<CoachProfile> coaches = coachProfileRepository.findAll();
        return ResponseEntity.ok(coaches);
    }

    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents() {
        List<StudentProfile> students = studentProfileRepository.findAll();
        return ResponseEntity.ok(students);
    }

    public static class FlagRequest {
        public String flaggedField;
        public String reasonNote;
    }

    // Flag a coach profile
    @PostMapping("/coaches/{id}/flag")
    public ResponseEntity<?> flagCoach(@PathVariable Long id, @RequestBody FlagRequest req) {
        try {
            adminService.flagCoachProfile(id, req.flaggedField, req.reasonNote);
            return ResponseEntity.ok("Profile flagged successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/coaches/{id}/approve")
    public ResponseEntity<?> approveCoach(@PathVariable Long id) {
        try {
            adminService.approveCoachProfile(id);
            return ResponseEntity.ok("Profile approved successfully and is now LIVE.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/coaches/{id}/feature")
    public ResponseEntity<?> toggleFeatureCoach(@PathVariable Long id) {
        try {
            CoachProfile coach = coachRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Coach not found"));
            coach.setIsFeatured(coach.getIsFeatured() == null || !coach.getIsFeatured());
            coachRepository.save(coach);
            return ResponseEntity
                    .ok(coach.getIsFeatured() ? "Coach featured successfully!" : "Coach removed from featured.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/coaches/{id}/reject")
    public ResponseEntity<?> rejectCoachProfile(@PathVariable Long id) {
        try {
            adminService.rejectCoachProfile(id);
            return ResponseEntity.ok("Coach profile rejected.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/coaches/{id}")
    public ResponseEntity<?> deleteCoach(@PathVariable Long id) {
        try {
            adminService.deleteCoachProfile(id);
            return ResponseEntity.ok("Coach profile and account deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/coaches/{id}/toggle-active")
    public ResponseEntity<?> toggleActiveCoach(@PathVariable Long id) {
        try {
            adminService.toggleActiveCoachProfile(id);
            return ResponseEntity.ok("Coach active status toggled.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/students/{id}/flag")
    public ResponseEntity<?> flagStudent(@PathVariable Long id, @RequestBody FlagRequest req) {
        try {
            adminService.flagStudentProfile(id, req.flaggedField, req.reasonNote);
            return ResponseEntity.ok("Student profile flagged successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/students/{id}/approve")
    public ResponseEntity<?> approveStudent(@PathVariable Long id) {
        try {
            adminService.approveStudentProfile(id);
            return ResponseEntity.ok("Student profile approved successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
