package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.StudentProfileRepository;
import com.coachkonnects.backend.repository.UserRepository;
import com.coachkonnects.backend.repository.IpTrackingRepository;
import com.coachkonnects.backend.model.IpTracking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IpTrackingRepository ipTrackingRepository;

    private static final List<String> SPAM_WORDS = Arrays.asList(
        "math", "science", "physics", "chemistry", // Educational Subjects not allowed
        "scam", "hack", "crypto", "bitcoin", "investment", // Spam
        "idiot", "stupid", "dumb" // Negative Words
    );

    public static class ProfileRequest {
        public String email;
        public String fullName;
        public String mobile;
        public String dob;
        public String district;
        public String state;
        public String pincode;
        public String area;
        public String location;
    }

    @GetMapping("/coach")
    public ResponseEntity<?> getAllCoaches() {
        return ResponseEntity.ok(coachProfileRepository.findAll());
    }

    @PostMapping("/coach")
    public ResponseEntity<?> createCoachProfile(@RequestBody ProfileRequest req, HttpServletRequest request) {
        try {
            String ip = request.getRemoteAddr();
            checkIpLimit(ip);
            checkSpamPolicy(req.fullName + " " + req.district + " " + req.state);

            User user = userRepository.findByEmail(req.email)
                    .orElseThrow(() -> new RuntimeException("User not found. Please verify email first."));

            CoachProfile profile = new CoachProfile();
            profile.setUser(user);
            profile.setFullName(req.fullName);
            profile.setDateOfBirth(req.dob);
            profile.setDistrict(req.district);
            profile.setState(req.state);
            profile.setPincode(req.pincode);
            profile.setArea(req.area);
            profile.setLocation(req.location);

            String baseSlug = req.fullName.toLowerCase().replace(" ", "-") + "-coach";
            profile.setSlug(baseSlug + "-" + System.currentTimeMillis());

            CoachProfile saved = coachProfileRepository.save(profile);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving coach profile: " + e.getMessage());
        }
    }

    @PostMapping("/student")
    public ResponseEntity<?> createStudentProfile(@RequestBody ProfileRequest req, HttpServletRequest request) {
        try {
            String ip = request.getRemoteAddr();
            checkIpLimit(ip);
            checkSpamPolicy(req.fullName + " " + req.district + " " + req.state);

            User user = userRepository.findByEmail(req.email)
                    .orElseThrow(() -> new RuntimeException("User not found. Please verify email first."));

            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            profile.setFullName(req.fullName);
            profile.setDateOfBirth(req.dob);
            profile.setDistrict(req.district);
            profile.setState(req.state);
            profile.setPincode(req.pincode);
            profile.setArea(req.area);
            profile.setLocation(req.location);

            StudentProfile saved = studentProfileRepository.save(profile);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void checkIpLimit(String ip) {
        IpTracking tracker = ipTrackingRepository.findById(ip).orElseGet(() -> {
            IpTracking newTracker = new IpTracking();
            newTracker.setIpAddress(ip);
            return newTracker;
        });

        if (tracker.isBlocked()) {
            throw new RuntimeException("IP_BLOCKED: This IP address has been blocked due to suspicious activity.");
        }

        if (tracker.getRegistrationCount() >= 5) {
            tracker.setBlocked(true);
            ipTrackingRepository.save(tracker);
            throw new RuntimeException("IP_LIMIT_EXCEEDED: Maximum 5 registrations allowed per IP address.");
        }

        tracker.setRegistrationCount(tracker.getRegistrationCount() + 1);
        tracker.setLastAttemptAt(LocalDateTime.now());
        ipTrackingRepository.save(tracker);
    }

    private void checkSpamPolicy(String content) {
        if (content == null) return;
        String lowerContent = content.toLowerCase();
        for (String word : SPAM_WORDS) {
            if (lowerContent.contains(word)) {
                throw new RuntimeException("SPAM_DETECTED: Registration blocked due to policy violation (Spam/Educational Subjects).");
            }
        }
    }
}
