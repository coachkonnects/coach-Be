package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.Review;
import com.coachkonnects.backend.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByCoachIdOrderByCreatedAtDesc(Long coachId);

    List<Review> findByCoachIdAndStatusOrderByCreatedAtDesc(Long coachId, ReviewStatus status);
    
    boolean existsByCoachIdAndStudentId(Long coachId, Long studentId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.coach.id = :coachId AND r.status = APPROVED")
    Double getAverageRatingForCoach(@Param("coachId") Long coachId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.coach.id = :coachId AND r.status = APPROVED")
    Long getReviewCountForCoach(@Param("coachId") Long coachId);
}
