package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    java.util.List<Category> findByIsApprovedTrue();
    java.util.List<Category> findByIsApprovedFalse();
}
