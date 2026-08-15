package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.Enquiry;
import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    List<Enquiry> findByCoach(CoachProfile coach);
    List<Enquiry> findByStudent(StudentProfile student);
    List<Enquiry> findByLeadEmail(String leadEmail);
}
