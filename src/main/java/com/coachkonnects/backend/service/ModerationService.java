package com.coachkonnects.backend.service;

import com.coachkonnects.backend.model.BlockedWord;
import com.coachkonnects.backend.repository.BlockedWordRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ModerationService {

    @Autowired
    private BlockedWordRepository blockedWordRepository;

    private List<BlockedWord> cachedBlockedWords;

    @PostConstruct
    public void init() {
        // Seed default words if table is empty
        if (blockedWordRepository.count() == 0) {
            List<BlockedWord> defaults = Arrays.asList(
                new BlockedWord("couple", "ROMANTIC"),
                new BlockedWord("date night", "ROMANTIC"),
                new BlockedWord("romantic", "ROMANTIC"),
                new BlockedWord("just the two of you", "ROMANTIC"),
                new BlockedWord("firearm", "WEAPONS"),
                new BlockedWord("gun training", "WEAPONS"),
                new BlockedWord("knife fighting", "WEAPONS"),
                new BlockedWord("combat training", "WEAPONS"),
                new BlockedWord("therapy", "MEDICAL"),
                new BlockedWord("healing", "MEDICAL"),
                new BlockedWord("treatment", "MEDICAL"),
                new BlockedWord("cure", "MEDICAL"),
                new BlockedWord("cures", "MEDICAL"),
                new BlockedWord("diagnosis", "MEDICAL"),
                new BlockedWord("medically proven", "MEDICAL"),
                new BlockedWord("convert", "RELIGIOUS"),
                new BlockedWord("true faith", "RELIGIOUS"),
                new BlockedWord("join our faith", "RELIGIOUS"),
                new BlockedWord("private home visit", "UNSUPERVISED"),
                new BlockedWord("one-on-one at my place", "UNSUPERVISED"),
                new BlockedWord("come alone", "UNSUPERVISED"),
                new BlockedWord("just us", "UNSUPERVISED"),
                new BlockedWord("whatsapp me", "CONTACT_BYPASS"),
                new BlockedWord("insta:", "CONTACT_BYPASS"),
                new BlockedWord("pay directly", "PAYMENT_BYPASS"),
                new BlockedWord("cash only", "PAYMENT_BYPASS"),
                new BlockedWord("outside the app", "PAYMENT_BYPASS"),
                new BlockedWord("skip the fee", "PAYMENT_BYPASS")
            );
            blockedWordRepository.saveAll(defaults);
        }
        
        cachedBlockedWords = blockedWordRepository.findAll();
    }

    public void reloadCache() {
        cachedBlockedWords = blockedWordRepository.findAll();
    }

    public void validateContent(String content) {
        if (content == null || content.isEmpty()) return;
        
        String lowerContent = content.toLowerCase();
        
        for (BlockedWord bw : cachedBlockedWords) {
            String target = bw.getWord().toLowerCase();
            // Simple exact substring match
            if (lowerContent.contains(target)) {
                // If the match is a standalone word, we block it to reduce false positives
                // e.g. "couple" vs "decoupled"
                String regex = "\\b" + Pattern.quote(target) + "\\b";
                if (Pattern.compile(regex).matcher(lowerContent).find()) {
                    throw new RuntimeException("CONTENT_BLOCKED: Your description contains restricted language (" + bw.getCategory() + " policy violation).");
                }
            }
        }
    }
}
