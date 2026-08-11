package com.coachkonnects.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_flags")
public class AdminFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String flaggedField; // Which module or field was flagged

    @Column(nullable = false, length = 1000)
    private String reasonNote; // The note written by admin for the user

    @Column(nullable = false)
    private boolean isResolved = false;

    // Track how many times this specific issue has been repeatedly rejected
    @Column(nullable = false)
    private int strikeCount = 1;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFlaggedField() { return flaggedField; }
    public void setFlaggedField(String flaggedField) { this.flaggedField = flaggedField; }
    public String getReasonNote() { return reasonNote; }
    public void setReasonNote(String reasonNote) { this.reasonNote = reasonNote; }
    public boolean isResolved() { return isResolved; }
    public void setResolved(boolean resolved) { this.isResolved = resolved; }
    public int getStrikeCount() { return strikeCount; }
    public void setStrikeCount(int strikeCount) { this.strikeCount = strikeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
