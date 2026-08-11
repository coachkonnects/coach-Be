package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.CoachProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachProfileRepository extends JpaRepository<CoachProfile, Long> {
}
