package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.coachkonnects.backend.model.User;
import java.util.Optional;

public interface CoachProfileRepository extends JpaRepository<CoachProfile, Long> {
    List<CoachProfile> findByStatusAndIsActiveTrue(ProfileStatus status);
    List<CoachProfile> findByIsActiveTrue();
    Optional<CoachProfile> findByUser(User user);
    Optional<CoachProfile> findBySlugAndStatusAndIsActiveTrue(String slug, ProfileStatus status);
    Optional<CoachProfile> findBySlugAndIsActiveTrue(String slug);
    Optional<CoachProfile> findBySocialLinks(String socialLinks);
}
