package com.coachkonnects.backend.repository;
import com.coachkonnects.backend.model.CoachAvailability;
import com.coachkonnects.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoachAvailabilityRepository extends JpaRepository<CoachAvailability, Long> {
    List<CoachAvailability> findByUser(User user);
    void deleteByUser(User user);
}
