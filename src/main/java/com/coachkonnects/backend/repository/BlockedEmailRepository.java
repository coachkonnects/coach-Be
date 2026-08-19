package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.BlockedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedEmailRepository extends JpaRepository<BlockedEmail, Long> {
    Optional<BlockedEmail> findByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    
    @org.springframework.transaction.annotation.Transactional
    void deleteByEmailIgnoreCase(String email);
}
