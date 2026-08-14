package com.coachkonnects.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enquiries")
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lead contact info stored directly — no StudentProfile needed
    private String leadName;
    private String leadEmail;
    private String leadPhone;
    private String leadLocation;

    // Optional: still link to a real StudentProfile if they are a registered user
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = true)
    private StudentProfile student;

    @ManyToOne
    @JoinColumn(name = "coach_id", nullable = false)
    private CoachProfile coach;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryStatus status = EnquiryStatus.PENDING_COACH_APPROVAL;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }

    public String getLeadEmail() { return leadEmail; }
    public void setLeadEmail(String leadEmail) { this.leadEmail = leadEmail; }

    public String getLeadPhone() { return leadPhone; }
    public void setLeadPhone(String leadPhone) { this.leadPhone = leadPhone; }

    public String getLeadLocation() { return leadLocation; }
    public void setLeadLocation(String leadLocation) { this.leadLocation = leadLocation; }

    public StudentProfile getStudent() { return student; }
    public void setStudent(StudentProfile student) { this.student = student; }

    public CoachProfile getCoach() { return coach; }
    public void setCoach(CoachProfile coach) { this.coach = coach; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public EnquiryStatus getStatus() { return status; }
    public void setStatus(EnquiryStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
