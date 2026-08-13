package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.StudentProfileRepository;
import com.coachkonnects.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AdminService adminService;

    // Get all coaches (Pending, Approved, Rejected) for the dashboard
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
