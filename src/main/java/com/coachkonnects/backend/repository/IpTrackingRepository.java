package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.IpTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IpTrackingRepository extends JpaRepository<IpTracking, String> {
}
