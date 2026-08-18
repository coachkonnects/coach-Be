package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.BlockedWord;
import com.coachkonnects.backend.repository.BlockedWordRepository;
import com.coachkonnects.backend.service.ModerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SecurityConfigController {

    @Autowired
    private BlockedWordRepository blockedWordRepository;

    @Autowired
    private ModerationService moderationService;

    @GetMapping("/config/blocked-words")
    public ResponseEntity<List<String>> getBlockedWords() {
        List<String> words = blockedWordRepository.findAll().stream()
                .map(BlockedWord::getWord)
                .collect(Collectors.toList());
        return ResponseEntity.ok(words);
    }

    @PostMapping("/admin/security/blocked-words")
    public ResponseEntity<?> addBlockedWord(@RequestBody Map<String, String> body) {
        String wordStr = body.get("word");
        String category = body.getOrDefault("category", "CUSTOM");
        
        if (wordStr == null || wordStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Word cannot be empty"));
        }
        
        String[] words = wordStr.split(",");
        List<BlockedWord> allWords = blockedWordRepository.findAll();
        List<String> existingWords = allWords.stream().map(BlockedWord::getWord).collect(Collectors.toList());
        
        List<BlockedWord> addedWords = new ArrayList<>();
        
        for (String w : words) {
            String cleanWord = w.trim();
            if (cleanWord.isEmpty() || existingWords.contains(cleanWord)) {
                continue;
            }
            BlockedWord bw = new BlockedWord(cleanWord, category);
            blockedWordRepository.save(bw);
            addedWords.add(bw);
            existingWords.add(cleanWord);
        }
        
        if (addedWords.isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of("error", "All words provided already exist or were invalid"));
        }
        
        moderationService.reloadCache();
        return ResponseEntity.ok(Map.of("message", addedWords.size() + " words added successfully"));
    }

    @DeleteMapping("/admin/security/blocked-words/{id}")
    public ResponseEntity<?> deleteBlockedWord(@PathVariable Long id) {
        if (!blockedWordRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        blockedWordRepository.deleteById(id);
        moderationService.reloadCache();
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/admin/security/blocked-words/all")
    public ResponseEntity<?> getAllBlockedWordsForAdmin() {
        return ResponseEntity.ok(blockedWordRepository.findAll());
    }
}
