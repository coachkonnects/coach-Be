package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.Category;
import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.repository.CategoryRepository;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return categoryRepository.findByIsApprovedTrue();
    }

    @GetMapping("/admin/categories")
    public List<Category> getAdminCategories() {
        return categoryRepository.findAll();
    }

    static class CategoryRequest {
        public String name;
        public String expertises;
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody CategoryRequest req) {
        if (req.name == null || req.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        Category cat = new Category(req.name.trim());
        if (req.expertises != null) {
            cat.setExpertises(req.expertises.trim());
        }
        categoryRepository.save(cat);
        return ResponseEntity.ok(cat);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PutMapping("/admin/categories/{id}/approve")
    public ResponseEntity<?> approveCategory(@PathVariable Long id) {
        return categoryRepository.findById(id).map(cat -> {
            cat.setApproved(true);
            categoryRepository.save(cat);
            return ResponseEntity.ok(cat);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/categories/{id}/edit")
    public ResponseEntity<?> editCategory(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newName = payload.get("name");
        String newExpertises = payload.get("expertises");
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        return categoryRepository.findById(id).map(cat -> {
            cat.setName(newName.trim());
            if (newExpertises != null) {
                cat.setExpertises(newExpertises.trim());
            } else if (payload.containsKey("expertises")) {
                cat.setExpertises("");
            }
            categoryRepository.save(cat);
            return ResponseEntity.ok(cat);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/categories/{id}/merge")
    public ResponseEntity<?> mergeCategory(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String targetName = payload.get("targetName");
        if (targetName == null || targetName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Target name is required");
        }
        return categoryRepository.findById(id).map(pendingCat -> {
            // Find coaches with pending category
            List<CoachProfile> coaches = coachProfileRepository.findAll();
            for (CoachProfile coach : coaches) {
                if (pendingCat.getName().equalsIgnoreCase(coach.getCategory())) {
                    coach.setCategory(targetName.trim());
                    coachProfileRepository.save(coach);
                }
            }
            categoryRepository.delete(pendingCat);
            return ResponseEntity.ok(Map.of("message", "Merged successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
