package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.CoachClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private CoachClassRepository coachClassRepository;

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

    @GetMapping("/coach/{slug}/classes")
    public ResponseEntity<?> getPublicClassesByCoachSlug(@PathVariable String slug) {
        Optional<CoachProfile> coachOpt = coachProfileRepository.findBySlugAndStatusAndIsActiveTrue(slug, ProfileStatus.APPROVED);
        if (coachOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(coachClassRepository.findByUser(coachOpt.get().getUser()));
    }

    @GetMapping("/classes")
    public ResponseEntity<?> getPublicClasses() {
        return ResponseEntity.ok(coachClassRepository.findAll());
    }
}
