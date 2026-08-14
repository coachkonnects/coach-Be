package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.BlockedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedWordRepository extends JpaRepository<BlockedWord, Long> {
}
