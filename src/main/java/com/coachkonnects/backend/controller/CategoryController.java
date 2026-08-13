package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.Category;
import com.coachkonnects.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    static class CategoryRequest {
        public String name;
    }

    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody CategoryRequest req) {
        if (req.name == null || req.name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        Category cat = new Category(req.name.trim());
        categoryRepository.save(cat);
        return ResponseEntity.ok(cat);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}
