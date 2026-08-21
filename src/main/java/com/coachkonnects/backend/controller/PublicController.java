package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.CoachClassRepository;
import com.coachkonnects.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    @Autowired
    private ReviewRepository reviewRepository;


    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private CoachClassRepository coachClassRepository;

    @GetMapping("/coaches")
    public ResponseEntity<?> getPublicCoaches() {
        List<CoachProfile> approvedCoaches = coachProfileRepository.findByStatusAndIsActiveTrue(ProfileStatus.APPROVED);
        approvedCoaches.forEach(c -> {
            Double avg = reviewRepository.getAverageRatingForCoach(c.getId());
            Long count = reviewRepository.getReviewCountForCoach(c.getId());
            c.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            c.setReviewCount(count != null ? count : 0L);
        });
        return ResponseEntity.ok(approvedCoaches);
    }

    @GetMapping("/coach/{slug}")
    public ResponseEntity<?> getPublicCoachBySlug(@PathVariable String slug) {
        Optional<CoachProfile> coachOpt;
        try {
            Long id = Long.parseLong(slug);
            coachOpt = coachProfileRepository.findById(id).filter(c -> c.getStatus() == ProfileStatus.APPROVED && c.isActive());
        } catch (NumberFormatException e) {
            coachOpt = coachProfileRepository.findBySlugAndStatusAndIsActiveTrue(slug, ProfileStatus.APPROVED);
        }
        return coachOpt.map(c -> {
            Double avg = reviewRepository.getAverageRatingForCoach(c.getId());
            Long count = reviewRepository.getReviewCountForCoach(c.getId());
            c.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            c.setReviewCount(count != null ? count : 0L);
            return ResponseEntity.ok(c);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/coach/{slug}/classes")
    public ResponseEntity<?> getPublicClassesByCoachSlug(@PathVariable String slug) {
        Optional<CoachProfile> coachOpt;
        try {
            Long id = Long.parseLong(slug);
            coachOpt = coachProfileRepository.findById(id).filter(c -> c.getStatus() == ProfileStatus.APPROVED && c.isActive());
        } catch (NumberFormatException e) {
            coachOpt = coachProfileRepository.findBySlugAndStatusAndIsActiveTrue(slug, ProfileStatus.APPROVED);
        }
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
