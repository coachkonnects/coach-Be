package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    Optional<StudentProfile> findByConsentToken(String consentToken);
}
