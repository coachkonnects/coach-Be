package com.coachkonnects.backend.controller;
import com.coachkonnects.backend.model.CoachClass;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachClassRepository;
import com.coachkonnects.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {
    
    @Autowired
    private CoachClassRepository classRepository;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createClass(@RequestParam String email, @RequestBody CoachClass classReq) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        classReq.setUser(user);
        return ResponseEntity.ok(classRepository.save(classReq));
    }

    @GetMapping
    public ResponseEntity<?> getMyClasses(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(classRepository.findByUser(user));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable Long id, @RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        CoachClass coachClass = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        if (!coachClass.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        classRepository.delete(coachClass);
        return ResponseEntity.ok().build();
    }
}
