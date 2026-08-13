package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.AdminFlag;
import com.coachkonnects.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminFlagRepository extends JpaRepository<AdminFlag, Long> {
    Optional<AdminFlag> findByUserAndFlaggedFieldAndIsResolvedFalse(User user, String flaggedField);
    java.util.List<AdminFlag> findByUserAndIsResolvedFalse(User user);
}
