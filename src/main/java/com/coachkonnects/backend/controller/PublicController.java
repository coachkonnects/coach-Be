package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.CoachClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @GetMapping("/coaches")
    public ResponseEntity<?> getPublicCoaches() {
        List<CoachProfile> approvedCoaches = coachProfileRepository.findByStatusAndIsActiveTrue(ProfileStatus.APPROVED);
        return ResponseEntity.ok(approvedCoaches);
    }

    @GetMapping("/coach/{slug}")
    public ResponseEntity<?> getPublicCoachBySlug(@PathVariable String slug) {
        return coachProfileRepository.findBySlugAndStatusAndIsActiveTrue(slug, ProfileStatus.APPROVED)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
