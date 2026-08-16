package com.coachkonnects.backend.controller;
import com.coachkonnects.backend.model.CoachClass;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.CoachClassRepository;
import com.coachkonnects.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/classes")
public class ClassController {
    
    @Autowired
    private CoachClassRepository classRepository;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createClass(HttpServletRequest request, @RequestBody CoachClass classReq) {
        String email = (String) request.getAttribute("userEmail");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        classReq.setUser(user);
        return ResponseEntity.ok(classRepository.save(classReq));
    }

    @GetMapping
    public ResponseEntity<?> getMyClasses(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(classRepository.findByUser(user));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClass(@PathVariable Long id, HttpServletRequest request, @RequestBody CoachClass classReq) {
        String email = (String) request.getAttribute("userEmail");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        CoachClass coachClass = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        if (!coachClass.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        coachClass.setTitle(classReq.getTitle());
        coachClass.setDescription(classReq.getDescription());
        coachClass.setSchedule(classReq.getSchedule());
        coachClass.setPrice(classReq.getPrice());
        coachClass.setCapacity(classReq.getCapacity());
        coachClass.setType(classReq.getType());
        coachClass.setImageUrl(classReq.getImageUrl());
        coachClass.setStatus(com.coachkonnects.backend.model.ProfileStatus.PENDING_APPROVAL);
        coachClass.setRejectReason(null);
        return ResponseEntity.ok(classRepository.save(coachClass));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable Long id, HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        CoachClass coachClass = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        if (!coachClass.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        classRepository.delete(coachClass);
        return ResponseEntity.ok().build();
    }
}
