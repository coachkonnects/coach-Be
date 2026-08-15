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
import com.coachkonnects.backend.repository.EnquiryRepository;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private com.coachkonnects.backend.service.ModerationService moderationService;

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
        public String gender;
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
        public String interests;
        public String preference;
        public String heardFrom;
        public Boolean parentalConsent;
        public String parentName;
        public String parentContact;

    }

    @GetMapping("/coach")
    public ResponseEntity<?> getAllCoaches() {
        return ResponseEntity.ok(coachProfileRepository.findAll());
    }

    @GetMapping("/coach/live")
    public ResponseEntity<?> getLiveCoaches() {
        return ResponseEntity.ok(coachProfileRepository.findByStatusAndIsActiveTrue(ProfileStatus.APPROVED));
    }

    @GetMapping("/student")
    public ResponseEntity<?> getAllStudents() {
        return ResponseEntity.ok(studentProfileRepository.findAll());
    }

    @DeleteMapping("/student/{id}")
    public ResponseEntity<?> deleteStudentProfile(@PathVariable Long id) {
        try {

            studentProfileRepository.findById(id).ifPresent(student -> {
                enquiryRepository.findByStudent(student).forEach(enquiry -> {
                    enquiry.setStudent(null);
                    enquiryRepository.save(enquiry);
                });
            });
            studentProfileRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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

            if (req.mobile != null && !req.mobile.isEmpty()) {
                user.setPhoneNumber(req.mobile);
                userRepository.save(user);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String changesJson = objectMapper.writeValueAsString(req);

            profile.setPendingChanges(changesJson);
            profile.setStatus(ProfileStatus.PENDING_APPROVAL);

            List<AdminFlag> activeFlags = adminFlagRepository.findByUserAndIsResolvedFalse(user);
            for (AdminFlag flag : activeFlags) {
                flag.setResolved(true);
                adminFlagRepository.save(flag);
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

    @GetMapping("/student/me")
    public ResponseEntity<?> getMyStudentProfile(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            StudentProfile profile = studentProfileRepository.findByUser(user).orElse(new StudentProfile());
            if (profile.getUser() == null) {
                profile.setUser(user);
            }

            return ResponseEntity.ok(Map.of("profile", profile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/student/me")
    public ResponseEntity<?> updateMyStudentProfile(@RequestParam String email, @RequestBody ProfileRequest req) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            StudentProfile profile = studentProfileRepository.findByUser(user).orElse(new StudentProfile());
            if (profile.getUser() == null) {
                profile.setUser(user);
            }

            if (req.fullName != null) profile.setFullName(req.fullName);
            if (req.dob != null) profile.setDateOfBirth(req.dob);
            if (req.gender != null) profile.setGender(req.gender);
            if (req.district != null) profile.setDistrict(req.district);
            if (req.state != null) profile.setState(req.state);
            if (req.pincode != null) profile.setPincode(req.pincode);
            if (req.area != null) profile.setArea(req.area);
            if (req.location != null) profile.setLocation(req.location);
            if (req.interests != null) profile.setInterests(req.interests);
            if (req.preference != null) profile.setPreference(req.preference);
            if (req.heardFrom != null) profile.setHeardFrom(req.heardFrom);
            if (req.parentalConsent != null) profile.setParentalConsent(req.parentalConsent);
            if (req.parentName != null) profile.setParentName(req.parentName);
            if (req.parentContact != null) profile.setParentContact(req.parentContact);


            profile.setStatus(ProfileStatus.APPROVED);
            profile.setRejectReason(null);

            StudentProfile saved = studentProfileRepository.save(profile);
            return ResponseEntity.ok(saved);
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

            if (coachProfileRepository.findByUser(user).isPresent()
                    || studentProfileRepository.findByUser(user).isPresent()) {
                throw new RuntimeException(
                        "This email is already registered with a profile. Please use a different email.");
            }

            if (req.mobile != null && !req.mobile.isEmpty()) {
                user.setPhoneNumber(req.mobile);
                userRepository.save(user);
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
            if (req.description != null) {
                moderationService.validateContent(req.description);
                profile.setDescription(req.description);
            }
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

            if (coachProfileRepository.findByUser(user).isPresent()
                    || studentProfileRepository.findByUser(user).isPresent()) {
                throw new RuntimeException(
                        "This email is already registered with a profile. Please use a different email.");
            }

            if (req.mobile != null && !req.mobile.isEmpty()) {
                user.setPhoneNumber(req.mobile);
                userRepository.save(user);
            }

            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            profile.setFullName(req.fullName);
            profile.setDateOfBirth(req.dob);
            profile.setGender(req.gender);
            profile.setDistrict(req.district);
            profile.setState(req.state);
            profile.setPincode(req.pincode);
            profile.setArea(req.area);
            profile.setLocation(req.location);
            profile.setInterests(req.interests);
            profile.setPreference(req.preference);
            profile.setHeardFrom(req.heardFrom);
            profile.setParentalConsent(req.parentalConsent);
            profile.setParentName(req.parentName);
            profile.setParentContact(req.parentContact);


            StudentProfile saved = studentProfileRepository.save(profile);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void checkIpLimit(String ip) {
        // Skip IP limit for local testing
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return;
        }

        IpTracking tracker = ipTrackingRepository.findById(ip).orElseGet(() -> {
            IpTracking newTracker = new IpTracking();
            newTracker.setIpAddress(ip);
            return newTracker;
        });

        if (tracker.getRegistrationCount() >= 5) {
            throw new RuntimeException("IP_LIMIT_EXCEEDED: Maximum 5 registrations allowed per IP address.");
        }

        tracker.setRegistrationCount(tracker.getRegistrationCount() + 1);
        tracker.setLastAttemptAt(LocalDateTime.now());
        ipTrackingRepository.save(tracker);
    }

    private void checkSpamPolicy(String content) {
        if (content == null)
            return;
        String lowerContent = content.toLowerCase();
        for (String word : SPAM_WORDS) {
            if (lowerContent.contains(word)) {
                throw new RuntimeException(
                        "SPAM_DETECTED: Registration blocked due to policy violation (Spam/Educational Subjects).");
            }
        }
    }
}
