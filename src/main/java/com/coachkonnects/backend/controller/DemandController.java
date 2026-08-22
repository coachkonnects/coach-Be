package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.Demand;
import com.coachkonnects.backend.repository.DemandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.coachkonnects.backend.util.JwtUtil;
import com.coachkonnects.backend.repository.UserRepository;

@RestController
@RequestMapping("/api")
public class DemandController {

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/public/demands")
    public ResponseEntity<?> submitDemand(@RequestBody Map<String, String> payload) {
        String skillName = payload.get("skillName");
        String location = payload.get("location");
        String email = payload.get("email");
        String mobileNumber = payload.get("mobileNumber");
        String pincode = payload.get("pincode");

        if (skillName == null || skillName.trim().isEmpty() || 
            location == null || location.trim().isEmpty() || 
            email == null || email.trim().isEmpty() ||
            mobileNumber == null || mobileNumber.trim().isEmpty() ||
            pincode == null || pincode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required."));
        }

        Demand demand = new Demand();
        demand.setSkillName(skillName.trim());
        demand.setLocation(location.trim());
        demand.setEmail(email.trim());
        demand.setMobileNumber(mobileNumber.trim());
        demand.setPincode(pincode.trim());
        demand.setApproved(false);
        demandRepository.save(demand);

        String token = null;
        com.coachkonnects.backend.model.User existingUserCheck = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (existingUserCheck == null) {
            com.coachkonnects.backend.model.User newUser = new com.coachkonnects.backend.model.User();
            newUser.setEmail(email.trim().toLowerCase());
            newUser.setRole("STUDENT");
            newUser.setPhoneNumber(mobileNumber.trim());
            userRepository.save(newUser);
            token = jwtUtil.generateToken(newUser.getEmail(), "STUDENT");
        }

        if (token != null) {
            return ResponseEntity.ok(Map.of("message", "Request submitted successfully.", "token", token));
        }

        return ResponseEntity.ok(Map.of("message", "Request submitted successfully."));
    }

    @GetMapping("/public/demands")
    public ResponseEntity<?> getPublicDemands() {
        List<Map<String, Object>> publicDemands = demandRepository.findByIsApprovedTrueOrderByCreatedAtDesc()
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

    @GetMapping("/admin/demands")
    public ResponseEntity<List<Demand>> getAdminDemands() {
        return ResponseEntity.ok(demandRepository.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/admin/demands/{id}/approve")
    public ResponseEntity<?> approveDemand(@PathVariable Long id) {
        return demandRepository.findById(id).map(demand -> {
            demand.setApproved(true);
            demandRepository.save(demand);
            return ResponseEntity.ok(demand);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/demands/{id}/edit")
    public ResponseEntity<?> editDemand(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newSkillName = payload.get("skillName");
        if (newSkillName == null || newSkillName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Skill Name is required");
        }
        return demandRepository.findById(id).map(demand -> {
            demand.setSkillName(newSkillName.trim());
            demandRepository.save(demand);
            return ResponseEntity.ok(demand);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/demands/{id}")
    public ResponseEntity<?> deleteDemand(@PathVariable Long id) {
        return demandRepository.findById(id).map(demand -> {
            demandRepository.delete(demand);
            return ResponseEntity.ok(Map.of("message", "Demand deleted successfully."));
        }).orElse(ResponseEntity.notFound().build());
    }
}
