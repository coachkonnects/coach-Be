package com.coachkonnects.backend.service;

import com.coachkonnects.backend.model.*;
import com.coachkonnects.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

import java.util.Optional;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private AdminFlagRepository flagRepository;

    @Autowired
    private CoachProfileRepository coachRepository;

    @Autowired
    private StudentProfileRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private BlockedEmailRepository blockedEmailRepository;

    @Autowired
    private JavaMailSender mailSender;

    private static final int STUDENT_MAX_STRIKES = 3;
    private static final int COACH_MAX_STRIKES = 3;

    public void flagCoachProfile(Long coachId, String flaggedField, String reasonNote) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));

        AdminFlag flag = getOrCreateFlag(coach.getUser(), flaggedField);
        flag.setReasonNote(reasonNote);
        flag.setResolved(false);

        if (flag.getStrikeCount() >= COACH_MAX_STRIKES) {
            coach.setStatus(ProfileStatus.REJECTED);
        } else {
            coach.setStatus(ProfileStatus.REQUEST_CHANGE);
            flag.setStrikeCount(flag.getStrikeCount() + 1);
        }

        flagRepository.save(flag);
        coachRepository.save(coach);

    }

    public void flagStudentProfile(Long studentId, String flaggedField, String reasonNote) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        AdminFlag flag = getOrCreateFlag(student.getUser(), flaggedField);
        flag.setReasonNote(reasonNote);
        flag.setResolved(false);

        if (flag.getStrikeCount() >= STUDENT_MAX_STRIKES) {
            student.setStatus(ProfileStatus.REJECTED);
        } else {
            student.setStatus(ProfileStatus.REQUEST_CHANGE);
            flag.setStrikeCount(flag.getStrikeCount() + 1);
        }

        flagRepository.save(flag);
        studentRepository.save(student);

    }

    private AdminFlag getOrCreateFlag(User user, String field) {
        return flagRepository.findByUserAndFlaggedFieldAndIsResolvedFalse(user, field)
                .orElseGet(() -> {
                    AdminFlag newFlag = new AdminFlag();
                    newFlag.setUser(user);
                    newFlag.setFlaggedField(field);
                    newFlag.setStrikeCount(0);
                    return newFlag;
                });
    }

    public void approveCoachProfile(Long coachId) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));

        // If there are pending changes, apply them now
        if (coach.getPendingChanges() != null && !coach.getPendingChanges().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.coachkonnects.backend.controller.ProfileController.ProfileRequest req = mapper.readValue(
                        coach.getPendingChanges(),
                        com.coachkonnects.backend.controller.ProfileController.ProfileRequest.class);

                if (req.fullName != null)
                    coach.setFullName(req.fullName);
                if (req.dob != null)
                    coach.setDateOfBirth(req.dob);
                if (req.gender != null)
                    coach.setGender(req.gender);
                if (req.district != null)
                    coach.setDistrict(req.district);
                if (req.state != null)
                    coach.setState(req.state);
                if (req.pincode != null)
                    coach.setPincode(req.pincode);
                if (req.area != null)
                    coach.setArea(req.area);
                if (req.location != null)
                    coach.setLocation(req.location);
                if (req.category != null)
                    coach.setCategory(req.category);
                if (req.expertise != null)
                    coach.setExpertise(req.expertise);
                if (req.description != null)
                    coach.setDescription(req.description);
                if (req.classMode != null)
                    coach.setClassMode(req.classMode);
                if (req.pricing != null)
                    coach.setPricing(req.pricing);
                if (req.targetAudience != null)
                    coach.setTargetAudience(req.targetAudience);
                if (req.availableDays != null)
                    coach.setAvailableDays(req.availableDays);
                if (req.timeSlots != null)
                    coach.setTimeSlots(req.timeSlots);
                if (req.profileImageUrl != null)
                    coach.setProfileImageUrl(req.profileImageUrl);
                if (req.groupImageUrl != null)
                    coach.setGroupImageUrl(req.groupImageUrl);
                if (req.introVideoUrl != null)
                    coach.setIntroVideoUrl(req.introVideoUrl);
                if (req.socialLinks != null)
                    coach.setSocialLinks(req.socialLinks);

                coach.setPendingChanges(null);
            } catch (Exception e) {
                System.err.println("Failed to apply pending changes: " + e.getMessage());
            }
        }

        coach.setStatus(ProfileStatus.APPROVED);
        coachRepository.save(coach);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("support@coachkonnects.com", "CoachKonnects Security");
            helper.setTo(coach.getUser().getEmail());
            helper.setSubject("Your Coach Profile is Approved!");

            String htmlContent = "<h1>Congratulations!</h1>" +
                    "<p>Your Coach Profile has been approved and is now LIVE on CoachKonnects.</p>" +
                    "<p>Students can now view your profile, register for your classes, and send you enquiries.</p>" +
                    "<p>Keep your availability and classes updated for the best experience.</p>" +
                    "<p><a href='https://coachkonnects.com/login' style='display:inline-block;padding:10px 20px;background-color:#F97316;color:white;text-decoration:none;border-radius:5px;'>Log In to Your Dashboard</a></p>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }
    }

    public void rejectCoachProfile(Long coachId) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));

        boolean wasEdit = coach.getPendingChanges() != null && !coach.getPendingChanges().isEmpty();

        coach.setPendingChanges(null);

        if (wasEdit) {
            coach.setStatus(ProfileStatus.APPROVED);
        } else {
            coach.setStatus(ProfileStatus.REJECTED);
        }

        coachRepository.save(coach);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("support@coachkonnects.com", "CoachKonnects Security");
            helper.setTo(coach.getUser().getEmail());
            helper.setSubject("Action Required: Coach Profile Update Rejected");

            String htmlContent = "<h1>Profile Update Rejected</h1>" +
                    "<p>Hello " + (coach.getFullName() != null ? coach.getFullName() : "Coach") + ",</p>" +
                    "<p>Your recent profile submission or update has been rejected by our moderation team.</p>" +
                    "<p>Please review your profile details and ensure they meet our community guidelines before resubmitting.</p>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
    }

    public void approveStudentProfile(Long studentId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.setStatus(ProfileStatus.APPROVED);
        student.setRejectReason(null);
        studentRepository.save(student);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("support@coachkonnects.com", "CoachKonnects Registration");
            helper.setTo(student.getUser().getEmail());
            helper.setSubject("Your Student Profile is Approved!");

            String htmlContent = "<h1>Welcome to CoachKonnects!</h1>" +
                    "<p>Your Student Profile has been approved by our team.</p>" +
                    "<p>You can now browse coach profiles and send enquiries to start learning.</p>" +
                    "<p><a href='https://coachkonnects.com/login' style='display:inline-block;padding:10px 20px;background-color:#F97316;color:white;text-decoration:none;border-radius:5px;'>Log In to Your Dashboard</a></p>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send student approval email: " + e.getMessage());
        }
    }

    public void rejectStudentProfile(Long studentId, String reason) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.setStatus(ProfileStatus.REJECTED);
        student.setRejectReason(reason);
        studentRepository.save(student);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("support@coachkonnects.com", "CoachKonnects Moderation");
            helper.setTo(student.getUser().getEmail());
            helper.setSubject("Action Required: Student Profile Registration");

            String htmlContent = "<h1>Action Required</h1>" +
                    "<p>Hello " + (student.getFullName() != null ? student.getFullName() : "Student") + ",</p>" +
                    "<p>We reviewed your student profile, but we need you to make some updates before we can approve it.</p>" +
                    "<p><b>Reason:</b> " + reason + "</p>" +
                    "<p>Please log in to your dashboard and update your profile to resubmit.</p>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send student rejection email: " + e.getMessage());
        }
    }

    public void deleteStudentProfile(Long studentId, boolean banEmail) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        User user = student.getUser();

        if (banEmail && user.getEmail() != null) {
            if (!blockedEmailRepository.existsByEmailIgnoreCase(user.getEmail())) {
                String fullName = student.getFullName();
                blockedEmailRepository.save(new BlockedEmail(user.getEmail().toLowerCase(), fullName));
            }
        }

        List<AdminFlag> flags = flagRepository.findByUserAndIsResolvedFalse(user);
        flagRepository.deleteAll(flags);
        
        List<com.coachkonnects.backend.model.Enquiry> enquiries = enquiryRepository.findByStudent(student);
        enquiryRepository.deleteAll(enquiries);
        
        studentRepository.delete(student);
        userRepository.delete(user);
    }

    public void deleteCoachProfile(Long coachId, boolean banEmail) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));

        User user = coach.getUser();

        if (banEmail && user.getEmail() != null) {
            if (!blockedEmailRepository.existsByEmailIgnoreCase(user.getEmail())) {
                String fullName = coach.getFullName();
                blockedEmailRepository.save(new BlockedEmail(user.getEmail().toLowerCase(), fullName));
            }
        }

        List<AdminFlag> flags = flagRepository.findByUserAndIsResolvedFalse(user);
        flagRepository.deleteAll(flags);

        List<com.coachkonnects.backend.model.Enquiry> enquiries = enquiryRepository.findByCoach(coach);
        enquiryRepository.deleteAll(enquiries);

        coachRepository.delete(coach);
        userRepository.delete(user);
    }

    public void toggleActiveCoachProfile(Long coachId) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));
        coach.setActive(!coach.isActive());
        coachRepository.save(coach);
    }

    public void updateCoachCategory(Long coachId, String category) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));
        coach.setCategory(category);
        coachRepository.save(coach);
    }

    public void updateCoachExpertise(Long coachId, String expertise) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));
        coach.setExpertise(expertise);
        coachRepository.save(coach);
    }

    public int reslugAllCoaches() {
        java.util.List<CoachProfile> allCoaches = coachRepository.findAll();
        int count = 0;
        for (CoachProfile coach : allCoaches) {
            String name = coach.getFullName() != null
                    ? coach.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("-$", "")
                    : "coach";
            String expertise = coach.getExpertise() != null
                    ? coach.getExpertise().toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("-$", "")
                    : "";
            String location = coach.getDistrict() != null
                    ? coach.getDistrict().toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("-$", "")
                    : "";

            StringBuilder newSlug = new StringBuilder(name);
            if (!expertise.isEmpty()) newSlug.append("-").append(expertise);
            if (!location.isEmpty()) newSlug.append("-").append(location);
            newSlug.append("-coach-").append(java.util.UUID.randomUUID().toString().substring(0, 4));

            coach.setSlug(newSlug.toString());
            coachRepository.save(coach);
            count++;
        }
        return count;
    }
}
