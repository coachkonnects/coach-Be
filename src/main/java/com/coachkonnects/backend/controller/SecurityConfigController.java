package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.BlockedWord;
import com.coachkonnects.backend.repository.BlockedWordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SecurityConfigController {

    @Autowired
    private BlockedWordRepository blockedWordRepository;

    @GetMapping("/config/blocked-words")
    public ResponseEntity<List<String>> getBlockedWords() {
        List<String> words = blockedWordRepository.findAll().stream()
                .map(BlockedWord::getWord)
                .collect(Collectors.toList());
        return ResponseEntity.ok(words);
    }

    @PostMapping("/admin/security/blocked-words")
    public ResponseEntity<?> addBlockedWord(@RequestBody Map<String, String> body) {
        String word = body.get("word");
        String category = body.getOrDefault("category", "CUSTOM");
        
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Word cannot be empty"));
        }
        word = word.trim();
        
        // Find if exists
        List<BlockedWord> allWords = blockedWordRepository.findAll();
        for (BlockedWord bw : allWords) {
            if (bw.getWord().equals(word)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Word already exists"));
            }
        }
        
        BlockedWord bw = new BlockedWord(word, category);
        blockedWordRepository.save(bw);
        return ResponseEntity.ok(bw);
    }

    @DeleteMapping("/admin/security/blocked-words/{id}")
    public ResponseEntity<?> deleteBlockedWord(@PathVariable Long id) {
        if (!blockedWordRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        blockedWordRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/admin/security/blocked-words/all")
    public ResponseEntity<?> getAllBlockedWordsForAdmin() {
        return ResponseEntity.ok(blockedWordRepository.findAll());
    }
}
