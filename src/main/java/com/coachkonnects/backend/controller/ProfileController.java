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
import com.coachkonnects.backend.model.AdminFlag;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.AdminFlagRepository;
import java.util.Map;
import java.util.HashMap;
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

    @Autowired
    private AdminFlagRepository adminFlagRepository;

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
        public String category;
        public String expertise;
        public String description;
        public String classMode;
        public String pricing;
        public String targetAudience;
        public String availableDays;
        public String timeSlots;
        public String profileImageUrl;
        public String groupImageUrl;
        public String introVideoUrl;
        public String socialLinks;
    }

    @GetMapping("/coach")
    public ResponseEntity<?> getAllCoaches() {
        return ResponseEntity.ok(coachProfileRepository.findAll());
    }

    @GetMapping("/coach/me")
    public ResponseEntity<?> getMyCoachProfile(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            CoachProfile profile = coachProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Coach profile not found."));

            List<AdminFlag> activeFlags = adminFlagRepository.findByUserAndIsResolvedFalse(user);

            Map<String, Object> response = new HashMap<>();
            response.put("profile", profile);
            response.put("flags", activeFlags);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/coach/me")
    public ResponseEntity<?> updateMyCoachProfile(@RequestParam String email, @RequestBody ProfileRequest req) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            CoachProfile profile = coachProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Coach profile not found."));

            // Update fields
            if (req.fullName != null) profile.setFullName(req.fullName);
            if (req.dob != null) profile.setDateOfBirth(req.dob);
            if (req.district != null) profile.setDistrict(req.district);
            if (req.state != null) profile.setState(req.state);
            if (req.pincode != null) profile.setPincode(req.pincode);
            if (req.area != null) profile.setArea(req.area);
            if (req.location != null) profile.setLocation(req.location);
            if (req.category != null) profile.setCategory(req.category);
            if (req.expertise != null) profile.setExpertise(req.expertise);
            if (req.description != null) profile.setDescription(req.description);
            if (req.classMode != null) profile.setClassMode(req.classMode);
            if (req.pricing != null) profile.setPricing(req.pricing);
            if (req.targetAudience != null) profile.setTargetAudience(req.targetAudience);
            if (req.availableDays != null) profile.setAvailableDays(req.availableDays);
            if (req.timeSlots != null) profile.setTimeSlots(req.timeSlots);
            if (req.profileImageUrl != null) profile.setProfileImageUrl(req.profileImageUrl);
            if (req.groupImageUrl != null) profile.setGroupImageUrl(req.groupImageUrl);
            if (req.introVideoUrl != null) profile.setIntroVideoUrl(req.introVideoUrl);
            if (req.socialLinks != null) profile.setSocialLinks(req.socialLinks);

            // Set back to pending if flagged
            if (profile.getStatus() == ProfileStatus.REQUEST_CHANGE) {
                profile.setStatus(ProfileStatus.PENDING_APPROVAL);
                // Mark flags as resolved
                List<AdminFlag> activeFlags = adminFlagRepository.findByUserAndIsResolvedFalse(user);
                for (AdminFlag flag : activeFlags) {
                    flag.setResolved(true);
                    adminFlagRepository.save(flag);
                }
            }

            CoachProfile saved = coachProfileRepository.save(profile);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/coach/toggle-active")
    public ResponseEntity<?> toggleActiveStatus(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            CoachProfile profile = coachProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Coach profile not found."));

            profile.setActive(!profile.isActive());
            coachProfileRepository.save(profile);
            
            return ResponseEntity.ok(Map.of("isActive", profile.isActive()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/coach")
    public ResponseEntity<?> createCoachProfile(@RequestBody ProfileRequest req, HttpServletRequest request) {
        try {
            String ip = request.getRemoteAddr();
            checkIpLimit(ip);
            checkSpamPolicy(req.fullName + " " + req.district + " " + req.state);

            User user = userRepository.findByEmail(req.email)
                    .orElseThrow(() -> new RuntimeException("User not found. Please verify email first."));

            if (coachProfileRepository.findByUser(user).isPresent() || studentProfileRepository.findByUser(user).isPresent()) {
                throw new RuntimeException("This email is already registered with a profile. Please use a different email.");
            }

            CoachProfile profile = new CoachProfile();
            profile.setUser(user);
            profile.setFullName(req.fullName);
            profile.setDateOfBirth(req.dob);
            profile.setDistrict(req.district);
            profile.setState(req.state);
            profile.setPincode(req.pincode);
            profile.setArea(req.area);
            profile.setLocation(req.location);
            profile.setCategory(req.category);
            profile.setExpertise(req.expertise);
            profile.setDescription(req.description);
            profile.setClassMode(req.classMode);
            profile.setPricing(req.pricing);
            profile.setTargetAudience(req.targetAudience);
            profile.setAvailableDays(req.availableDays);
            profile.setTimeSlots(req.timeSlots);
            profile.setProfileImageUrl(req.profileImageUrl);
            profile.setGroupImageUrl(req.groupImageUrl);
            profile.setIntroVideoUrl(req.introVideoUrl);
            profile.setSocialLinks(req.socialLinks);

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

            if (coachProfileRepository.findByUser(user).isPresent() || studentProfileRepository.findByUser(user).isPresent()) {
                throw new RuntimeException("This email is already registered with a profile. Please use a different email.");
            }

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
