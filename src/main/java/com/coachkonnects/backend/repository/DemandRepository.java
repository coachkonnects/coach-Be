package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.Demand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {
    List<Demand> findAllByOrderByCreatedAtDesc();
}
