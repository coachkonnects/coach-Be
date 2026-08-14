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
        coach.setStatus(ProfileStatus.APPROVED);
        coachRepository.save(coach);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("coachkonnects@gmail.com", "CoachKonnects Security");
            helper.setTo(coach.getUser().getEmail());
            helper.setSubject("Your Coach Profile is Approved!");
            
            String htmlContent = "<h1>Congratulations!</h1>" +
                "<p>Your Coach Profile has been approved and is now LIVE on CoachKonnects.</p>" +
                "<p>Students can now view your profile, register for your classes, and send you enquiries.</p>" +
                "<p>Keep your availability and classes updated for the best experience.</p>";
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }
    }

    public void rejectCoachProfile(Long coachId) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));
        coach.setStatus(ProfileStatus.REJECTED);
        coachRepository.save(coach);
    }


    public void approveStudentProfile(Long studentId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.setStatus(ProfileStatus.APPROVED);
        studentRepository.save(student);
    }

    public void deleteCoachProfile(Long coachId) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));

        User user = coach.getUser();

        List<AdminFlag> flags = flagRepository.findByUserAndIsResolvedFalse(user);
        flagRepository.deleteAll(flags);

        coachRepository.delete(coach);
        userRepository.delete(user);
    }

    public void toggleActiveCoachProfile(Long coachId) {
        CoachProfile coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));
        coach.setActive(!coach.isActive());
        coachRepository.save(coach);
    }
}
