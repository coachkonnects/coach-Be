package com.coachkonnects.backend.repository;
import com.coachkonnects.backend.model.CoachClass;
import com.coachkonnects.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoachClassRepository extends JpaRepository<CoachClass, Long> {
    List<CoachClass> findByUser(User user);
}
