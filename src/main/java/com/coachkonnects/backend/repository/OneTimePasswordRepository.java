package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.OneTimePassword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OneTimePasswordRepository extends JpaRepository<OneTimePassword, String> {
    List<OneTimePassword> findByUserIdAndUsedFalseOrderByCreatedAtDesc(String userId);
}
