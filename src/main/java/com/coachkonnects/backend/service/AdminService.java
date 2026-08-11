package com.coachkonnects.backend.service;

import com.coachkonnects.backend.model.*;
import com.coachkonnects.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminFlagRepository flagRepository;

    @Autowired
    private CoachProfileRepository coachRepository;

    @Autowired
    private StudentProfileRepository studentRepository;

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
    }

    public void approveStudentProfile(Long studentId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.setStatus(ProfileStatus.APPROVED);
        studentRepository.save(student);
    }
}
