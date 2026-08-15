package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.Demand;
import com.coachkonnects.backend.repository.DemandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DemandController {

    @Autowired
    private DemandRepository demandRepository;

    // Public endpoint to submit a new demand
    @PostMapping("/public/demands")
    public ResponseEntity<?> submitDemand(@RequestBody Map<String, String> payload) {
        String skillName = payload.get("skillName");
        String location = payload.get("location");
        String email = payload.get("email");

        if (skillName == null || skillName.trim().isEmpty() || 
            location == null || location.trim().isEmpty() || 
            email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required."));
        }

        Demand demand = new Demand();
        demand.setSkillName(skillName.trim());
        demand.setLocation(location.trim());
        demand.setEmail(email.trim());
        demandRepository.save(demand);

        return ResponseEntity.ok(Map.of("message", "Request submitted successfully."));
    }

    // Public endpoint to get demands (HIDES EMAIL)
    @GetMapping("/public/demands")
    public ResponseEntity<?> getPublicDemands() {
        List<Map<String, Object>> publicDemands = demandRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "skillName", d.getSkillName(),
                "location", d.getLocation(),
                "createdAt", d.getCreatedAt()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(publicDemands);
    }

    // Admin endpoint to get full demands
    @GetMapping("/admin/demands")
    public ResponseEntity<List<Demand>> getAdminDemands() {
        // Normally secured by Spring Security role/token check, but mapping handles logic for MVP
        return ResponseEntity.ok(demandRepository.findAllByOrderByCreatedAtDesc());
    }
}
