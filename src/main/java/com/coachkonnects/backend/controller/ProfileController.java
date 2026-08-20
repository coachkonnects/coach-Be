package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.CategoryRepository;
import com.coachkonnects.backend.model.Category;
import com.coachkonnects.backend.repository.StudentProfileRepository;
import com.coachkonnects.backend.repository.UserRepository;
import com.coachkonnects.backend.repository.IpTrackingRepository;
import com.coachkonnects.backend.model.IpTracking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.coachkonnects.backend.model.Demand;
import com.coachkonnects.backend.repository.DemandRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import com.coachkonnects.backend.model.AdminFlag;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.AdminFlagRepository;
import com.coachkonnects.backend.repository.EnquiryRepository;
import java.util.Map;
import java.util.Optional;

import java.util.HashMap;
import java.util.UUID;
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
    private CategoryRepository categoryRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private JavaMailSender mailSender;

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

    private final java.util.concurrent.ConcurrentHashMap<String, Integer> parentOtpRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> parentOtpTimestamps = new java.util.concurrent.ConcurrentHashMap<>();

    public static class ProfileRequest {
        public String email;
        public Long demandId; // Optional link to a specific demand
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
        public String parentEmail;

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
    public ResponseEntity<?> getMyCoachProfile(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("userEmail");
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
    public ResponseEntity<?> updateMyCoachProfile(HttpServletRequest request, @RequestBody ProfileRequest req) {
        try {
            if (req.mobile != null && !req.mobile.isEmpty() && !req.mobile.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            if (req.parentContact != null && !req.parentContact.isEmpty() && !req.parentContact.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            String email = (String) request.getAttribute("userEmail");
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
    public ResponseEntity<?> toggleActiveStatus(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("userEmail");
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
    public ResponseEntity<?> getMyStudentProfile(HttpServletRequest request,
            @RequestParam(required = false) String email) {
        try {
            if (email == null || email.isEmpty()) {
                email = (String) request.getAttribute("userEmail");
            }
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
    public ResponseEntity<?> updateMyStudentProfile(HttpServletRequest request, @RequestBody ProfileRequest req) {
        try {
            if (req.mobile != null && !req.mobile.isEmpty()) {
                if (!req.mobile.matches("^[6-9]\\d{9}$")) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Mobile number must be 10 digits and start with 6, 7, 8, or 9."));
                }
                
                String email = (String) request.getAttribute("userEmail");
                Optional<User> existingUser = userRepository.findByPhoneNumber(req.mobile);
                if (existingUser.isPresent() && !existingUser.get().getEmail().equals(email)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "This mobile number is already registered with another account."));
                }
            }
            if (req.parentContact != null && !req.parentContact.isEmpty() && !req.parentContact.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            String email = (String) request.getAttribute("userEmail");
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            if (req.mobile != null && !req.mobile.isEmpty()) {
                user.setPhoneNumber(req.mobile);
                userRepository.save(user);
            }

            StudentProfile profile = studentProfileRepository.findByUser(user).orElse(new StudentProfile());
            if (profile.getUser() == null) {
                profile.setUser(user);
            }

            if (req.fullName != null)
                profile.setFullName(req.fullName);
            if (req.parentEmail != null && req.parentEmail.equalsIgnoreCase(user.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent email cannot be the same as your student account email."));
            }

            if (req.dob != null && !req.dob.isEmpty()) {
                validateDateOfBirth(req.dob);
                profile.setDateOfBirth(req.dob);
            }
            if (req.gender != null)
                profile.setGender(req.gender);
            if (req.district != null)
                profile.setDistrict(req.district);
            if (req.state != null)
                profile.setState(req.state);
            if (req.pincode != null)
                profile.setPincode(req.pincode);
            if (req.area != null)
                profile.setArea(req.area);
            if (req.location != null)
                profile.setLocation(req.location);
            if (req.interests != null)
                profile.setInterests(req.interests);
            if (req.preference != null)
                profile.setPreference(req.preference);
            if (req.heardFrom != null)
                profile.setHeardFrom(req.heardFrom);
            if (req.parentalConsent != null)
                profile.setParentalConsent(req.parentalConsent);
            if (req.parentName != null)
                profile.setParentName(req.parentName);
            if (req.parentContact != null)
                profile.setParentContact(req.parentContact);
            if (req.parentEmail != null)
                profile.setParentEmail(req.parentEmail);

            boolean isUnder18 = false;
            try {
                if (profile.getDateOfBirth() != null && !profile.getDateOfBirth().isEmpty()) {
                    int birthYear = 0;
                    String dob = profile.getDateOfBirth();
                    if (dob.contains("/")) {
                        birthYear = Integer.parseInt(dob.split("/")[2]);
                    } else if (dob.contains("-")) {
                        String firstPart = dob.split("-")[0];
                        birthYear = firstPart.length() == 4 ? Integer.parseInt(firstPart)
                                : Integer.parseInt(dob.split("-")[2]);
                    }
                    int currentYear = java.time.LocalDateTime.now().getYear();
                    if (birthYear > 0 && (currentYear - birthYear < 18)) {
                        isUnder18 = true;
                    }
                }
            } catch (Exception e) {
            }

            if (isUnder18 && profile.getParentEmail() != null && !profile.getParentEmail().isEmpty()) {
                // Check if they need a new token (e.g. email changed or not verified yet)
                if (profile.getParentConsentVerified() == null || !profile.getParentConsentVerified()) {
                    String token = String.format("%06d", new java.util.Random().nextInt(1000000));
                    profile.setConsentToken(token);
                    sendParentalConsentEmail(profile.getParentEmail(), profile.getFullName(), token);
                    profile.setParentConsentVerified(false);
                }
            } else {
                profile.setParentConsentVerified(true);
            }

            if (isUnder18 && (profile.getParentConsentVerified() == null || !profile.getParentConsentVerified())) {
                profile.setStatus(ProfileStatus.PENDING_APPROVAL);
                profile.setRejectReason("Pending Parental Consent");
            } else {
                profile.setStatus(ProfileStatus.APPROVED);
                profile.setRejectReason(null);
            }

            StudentProfile saved = studentProfileRepository.save(profile);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/coach")
    public ResponseEntity<?> createCoachProfile(@RequestBody ProfileRequest req, HttpServletRequest request) {
        try {
            if (req.mobile != null && !req.mobile.isEmpty() && !req.mobile.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            if (req.parentContact != null && !req.parentContact.isEmpty() && !req.parentContact.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            String ip = request.getRemoteAddr();
            checkIpLimit(ip);
            checkSpamPolicy(req.fullName + " " + req.district + " " + req.state);

            if (req.parentEmail != null && req.parentEmail.equalsIgnoreCase(req.email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent email cannot be the same as your student account email."));
            }

            if (req.dob != null && !req.dob.isEmpty()) {
                validateDateOfBirth(req.dob);
            }

            User user = userRepository.findByEmail(req.email)
                    .orElseThrow(() -> new RuntimeException("User not found. Please verify email first."));

            if ("ADMIN".equalsIgnoreCase(user.getRole()) ||
                    coachProfileRepository.findByUser(user).isPresent()
                    || studentProfileRepository.findByUser(user).isPresent()) {
                throw new RuntimeException(
                        "This email is already registered with a profile. Please use a different email.");
            }

            if (req.mobile != null && !req.mobile.trim().isEmpty()) {
                java.util.Optional<User> existingMobile = userRepository.findByPhoneNumber(req.mobile.trim());
                if (existingMobile.isPresent() && !existingMobile.get().getId().equals(user.getId())) {
                    throw new RuntimeException(
                            "This phone number is already registered. Please use a different phone number.");
                }
            }

            if (req.socialLinks != null && !req.socialLinks.trim().isEmpty()) {
                java.util.Optional<com.coachkonnects.backend.model.CoachProfile> existingCoach = coachProfileRepository
                        .findBySocialLinks(req.socialLinks.trim());
                if (existingCoach.isPresent() && !existingCoach.get().getUser().getId().equals(user.getId())) {
                    throw new RuntimeException(
                            "This Instagram handle is already registered. Please use a different one.");
                }
            }

            if (req.mobile != null && !req.mobile.isEmpty()) {
                user.setPhoneNumber(req.mobile);
                userRepository.save(user);
            }

            if (req.category != null && !req.category.isEmpty()) {
                if (categoryRepository.findByName(req.category).isEmpty()) {
                    Category newCat = new Category();
                    newCat.setName(req.category);
                    newCat.setApproved(false);
                    categoryRepository.save(newCat);
                }
            }
            CoachProfile profile = new CoachProfile();
            profile.setUser(user);
            profile.setFullName(req.fullName);
            profile.setDateOfBirth(req.dob);
            profile.setGender(req.gender);
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

            String cleanName = req.fullName != null
                    ? req.fullName.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("-$", "")
                    : "coach";
            String cleanExpertise = req.expertise != null
                    ? req.expertise.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("-$",
                            "")
                    : "";
            String cleanLocation = req.district != null
                    ? req.district.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("-$", "")
                    : "";

            StringBuilder slugBuilder = new StringBuilder(cleanName);
            if (!cleanExpertise.isEmpty()) {
                slugBuilder.append("-").append(cleanExpertise);
            }
            if (!cleanLocation.isEmpty()) {
                slugBuilder.append("-").append(cleanLocation);
            }
            // Add a very short 4-character random string to guarantee uniqueness without
            // looking ugly
            slugBuilder.append("-coach-").append(java.util.UUID.randomUUID().toString().substring(0, 4));

            profile.setSlug(slugBuilder.toString());

            CoachProfile saved = coachProfileRepository.save(profile);

            if (req.demandId != null) {
                demandRepository.findById(req.demandId).ifPresent(demand -> {
                    try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom("support@coachkonnects.com", "CoachKonnects Alerts");
                        helper.setTo(demand.getEmail());
                        helper.setSubject("A Coach just applied for your request!");

                        String htmlContent = "<h1>Great News!</h1>"
                                + "<p>Hello,</p>"
                                + "<p>You recently requested a class for <b>" + demand.getSkillName() + "</b> in "
                                + demand.getLocation() + ".</p>"
                                + "<p>A coach named <b>" + profile.getFullName()
                                + "</b> has just applied to teach it!</p>"
                                + "<p>Their profile is currently undergoing verification by our team. You will be notified once they are approved and LIVE.</p>"
                                + "<br/><p>Best regards,<br/>The CoachKonnects Team</p>";

                        helper.setText(htmlContent, true);
                        mailSender.send(message);
                    } catch (Exception ex) {
                        System.err.println("Failed to send demand notification email: " + ex.getMessage());
                    }
                });
            }

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving coach profile: " + e.getMessage());
        }
    }

    @PostMapping("/student")
    public ResponseEntity<?> createStudentProfile(@RequestBody ProfileRequest req, HttpServletRequest request) {
        try {
            if (req.mobile != null && !req.mobile.isEmpty() && !req.mobile.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            if (req.parentContact != null && !req.parentContact.isEmpty() && !req.parentContact.matches("^[6-9]\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent mobile number must be 10 digits and start with 6, 7, 8, or 9."));
            }
            String ip = request.getRemoteAddr();
            checkIpLimit(ip);
            checkSpamPolicy(req.fullName + " " + req.district + " " + req.state);

            if (req.parentEmail != null && req.email != null && req.parentEmail.equalsIgnoreCase(req.email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent email cannot be the same as your student account email."));
            }

            if (req.dob != null && !req.dob.isEmpty()) {
                validateDateOfBirth(req.dob);
            }

            User user = userRepository.findByEmail(req.email)
                    .orElseThrow(() -> new RuntimeException("User not found. Please verify email first."));

            if ("ADMIN".equalsIgnoreCase(user.getRole()) ||
                    coachProfileRepository.findByUser(user).isPresent()
                    || studentProfileRepository.findByUser(user).isPresent()) {
                throw new RuntimeException(
                        "This email is already registered with a profile. Please use a different email.");
            }

            if (req.mobile != null && !req.mobile.trim().isEmpty()) {
                java.util.Optional<User> existingMobile = userRepository.findByPhoneNumber(req.mobile.trim());
                if (existingMobile.isPresent() && !existingMobile.get().getId().equals(user.getId())) {
                    throw new RuntimeException(
                            "This phone number is already registered. Please use a different phone number.");
                }
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
            profile.setParentEmail(req.parentEmail);

            boolean isUnder18 = false;
            try {
                if (req.parentEmail != null && req.parentEmail.equalsIgnoreCase(user.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent email cannot be the same as your student account email."));
            }

            if (req.dob != null && !req.dob.isEmpty()) {
                    int birthYear = 0;
                    if (req.dob.contains("/")) {
                        birthYear = Integer.parseInt(req.dob.split("/")[2]);
                    } else if (req.dob.contains("-")) {
                        String firstPart = req.dob.split("-")[0];
                        birthYear = firstPart.length() == 4 ? Integer.parseInt(firstPart)
                                : Integer.parseInt(req.dob.split("-")[2]);
                    }
                    int currentYear = java.time.LocalDateTime.now().getYear();
                    if (birthYear > 0 && (currentYear - birthYear < 18)) {
                        isUnder18 = true;
                    }
                }
            } catch (Exception e) {
            }

            if (isUnder18 && req.parentEmail != null && !req.parentEmail.isEmpty() && (profile.getParentConsentVerified() == null || !profile.getParentConsentVerified())) {
                String token = String.format("%06d", new java.util.Random().nextInt(1000000));
                profile.setConsentToken(token);
                profile.setParentConsentVerified(false);
                sendParentalConsentEmail(req.parentEmail, req.fullName, token);
            } else {
                profile.setParentConsentVerified(true);
            }

            if (isUnder18 && (profile.getParentConsentVerified() == null || !profile.getParentConsentVerified())) {
                profile.setStatus(ProfileStatus.PENDING_APPROVAL);
                profile.setRejectReason("Pending Parental Consent");
            } else {
                profile.setStatus(ProfileStatus.APPROVED);
                profile.setRejectReason(null);
            }

            studentProfileRepository.save(profile);
            return ResponseEntity.ok(Map.of("message", "Student profile created successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify-consent/{token}")
    public ResponseEntity<?> verifyConsent(@PathVariable String token) {
        return studentProfileRepository.findByConsentToken(token).map(profile -> {
            profile.setParentConsentVerified(true);
            profile.setConsentToken(null);
            profile.setStatus(ProfileStatus.APPROVED);
            profile.setRejectReason(null);
            studentProfileRepository.save(profile);
            return ResponseEntity.ok("Parental consent verified successfully!");
        }).orElse(ResponseEntity.badRequest().body("Invalid or expired token."));
    }

    @PostMapping("/verify-parent-otp")
    public ResponseEntity<?> verifyParentOtp(HttpServletRequest request, @RequestBody Map<String, String> req) {
        try {
            String email = (String) request.getAttribute("userEmail");
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            StudentProfile profile = studentProfileRepository.findByUser(user).orElseThrow(() -> new RuntimeException("Profile not found"));

            String otp = req.get("otp");
            if (otp == null || !otp.equals(profile.getConsentToken())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));
            }

            profile.setParentConsentVerified(true);
            profile.setConsentToken(null);
            profile.setStatus(ProfileStatus.APPROVED);
            profile.setRejectReason(null);
            studentProfileRepository.save(profile);

            return ResponseEntity.ok(Map.of("message", "Parental consent verified successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-parent-otp")
    public ResponseEntity<?> resendParentOtp(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("userEmail");
            
            String ip = request.getRemoteAddr();
            long now = System.currentTimeMillis();
            
            // Skip limit for local testing
            if (!"127.0.0.1".equals(ip) && !"0:0:0:0:0:0:0:1".equals(ip)) {
                if (parentOtpTimestamps.containsKey(email) && (now - parentOtpTimestamps.get(email)) > 2 * 60 * 60 * 1000) {
                    parentOtpRequests.remove(email);
                }
                int attempts = parentOtpRequests.getOrDefault(email, 0);
                if (attempts >= 5) {
                    return ResponseEntity.status(429).body(Map.of("error", "Maximum OTP attempts reached. Please try again after 2 hours."));
                }
                parentOtpRequests.put(email, attempts + 1);
                parentOtpTimestamps.put(email, now);
            }

            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            StudentProfile profile = studentProfileRepository.findByUser(user).orElseThrow(() -> new RuntimeException("Profile not found"));

            if (profile.getParentEmail() == null || profile.getParentEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No parent email found on profile"));
            }

            String token = String.format("%06d", new java.util.Random().nextInt(1000000));
            profile.setConsentToken(token);
            profile.setParentConsentVerified(false);
            studentProfileRepository.save(profile);
            
            sendParentalConsentEmail(profile.getParentEmail(), profile.getFullName(), token);

            return ResponseEntity.ok(Map.of("message", "OTP resent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private void sendParentalConsentEmail(String email, String studentName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("support@coachkonnects.com", "CoachKonnects");
            helper.setTo(email);
            helper.setSubject("Uh Oh! Your Kid Wants to Learn - CoachKonnects");
            helper.setText("<h1>We found your kid!</h1><p>Hi there,</p><p>Looks like <b>" + studentName
                    + "</b> is trying to sign up for CoachKonnects. We just need to make sure you're aware (😄).</p><p>Please share this 6-digit OTP with them to verify their account:</p><h2>"
                    + token
                    + "</h2><p>If you didn't expect this, just ignore this email and we'll keep them out of our classrooms!</p><p>- The CoachKonnects Team</p>",
                    true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
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

    private void validateDateOfBirth(String dobString) {
        if (dobString == null || dobString.trim().isEmpty()) {
            throw new RuntimeException("Date of Birth is required.");
        }
        try {
            int year = 0, month = 0, day = 0;
            if (dobString.contains("/")) {
                String[] parts = dobString.split("/");
                day = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
                year = Integer.parseInt(parts[2]);
            } else if (dobString.contains("-")) {
                String[] parts = dobString.split("-");
                if (parts[0].length() == 4) {
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                    day = Integer.parseInt(parts[2]);
                } else {
                    day = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                    year = Integer.parseInt(parts[2]);
                }
            } else {
                throw new RuntimeException("Invalid Date of Birth format.");
            }

            java.time.LocalDate dob = java.time.LocalDate.of(year, month, day);
            java.time.LocalDate today = java.time.LocalDate.now();

            if (dob.isAfter(today)) {
                throw new RuntimeException("Date of Birth cannot be in the future.");
            }

            java.time.LocalDate sixMonthsAgo = today.minusMonths(6);
            if (dob.isAfter(sixMonthsAgo)) {
                throw new RuntimeException("User must be at least 6 months old.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid Date of Birth.");
        }
    }
}
