package com.coachkonnects.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ip_tracking")
public class IpTracking {

    @Id
    @Column(name = "ip_address", length = 45) // Support for IPv6
    private String ipAddress;

    @Column(name = "registration_count")
    private int registrationCount = 0;

    @Column(name = "is_blocked")
    private boolean isBlocked = false;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt = LocalDateTime.now();

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getRegistrationCount() {
        return registrationCount;
    }

    public void setRegistrationCount(int registrationCount) {
        this.registrationCount = registrationCount;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
