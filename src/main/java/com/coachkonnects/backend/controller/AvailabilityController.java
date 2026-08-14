package com.coachkonnects.backend.controller;
import com.coachkonnects.backend.model.CoachAvailability;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachAvailabilityRepository;
import com.coachkonnects.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {
    
    @Autowired
    private CoachAvailabilityRepository availabilityRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAvailability(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(availabilityRepository.findByUser(user));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> updateAvailability(@RequestParam String email, @RequestBody List<CoachAvailability> availabilities) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        availabilityRepository.deleteByUser(user);
        for(CoachAvailability a : availabilities) {
            a.setUser(user);
        }
        return ResponseEntity.ok(availabilityRepository.saveAll(availabilities));
    }
}
