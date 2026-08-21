package com.coachkonnects.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "coach_profiles")
public class CoachProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    
    @Formula("(SELECT COUNT(e.id) FROM enquiries e WHERE e.coach_id = id AND e.status = 'APPROVED')")
    private Integer studentCount;

    public Integer getStudentCount() {
        return studentCount != null ? studentCount : 0;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String district;
    private String state;
    private String category;
    private String expertise;
    private String pincode;
    private String area;
    private String location;

    @Column(length = 2000)
    private String description;

    private String classMode;
    private String pricing;
    private Integer minPrice;
    private Integer maxPrice;
    private String targetAudience;

    private String availableDays;
    private String timeSlots;

    private String profileImageUrl;
    private String groupImageUrl;
    @jakarta.persistence.Column(name = "is_featured")
    private Boolean isFeatured = false;
    private String introVideoUrl;
    private String socialLinks;

    @Column(columnDefinition = "TEXT")
    private String pendingChanges;

    @Column(unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status = ProfileStatus.PENDING_APPROVAL;

    @Column(columnDefinition = "boolean default true")
    private boolean isActive = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public ProfileStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getClassMode() {
        return classMode;
    }

    public void setClassMode(String classMode) {
        this.classMode = classMode;
    }

    public String getPricing() {
        return pricing;
    }

    public void setPricing(String pricing) {
        this.pricing = pricing;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public String getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(String timeSlots) {
        this.timeSlots = timeSlots;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }


    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }

    public String getGroupImageUrl() {
        return groupImageUrl;
    }

    public void setGroupImageUrl(String groupImageUrl) {
        this.groupImageUrl = groupImageUrl;
    }

    public String getIntroVideoUrl() {
        return introVideoUrl;
    }

    public void setIntroVideoUrl(String introVideoUrl) {
        this.introVideoUrl = introVideoUrl;
    }

    public String getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(String socialLinks) {
        this.socialLinks = socialLinks;
    }

    public String getPendingChanges() {
        return pendingChanges;
    }

    public void setPendingChanges(String pendingChanges) {
        this.pendingChanges = pendingChanges;
    }
}
