package com.portfolio.profile.internal;

import com.portfolio.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/** The portfolio owner's public profile (a single row). */
@Entity
@Table(name = "profile")
class Profile extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 200)
    private String headline;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String bio;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(length = 120)
    private String location;

    @Column(name = "contact_email", length = 190)
    private String contactEmail;

    @Column(length = 160)
    private String availability;

    @Column(name = "github_url", length = 512)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 512)
    private String linkedinUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "resume_url", length = 1024)
    private String resumeUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position")
    @Column(name = "skill", length = 40, nullable = false)
    private List<String> skills = new ArrayList<>();

    protected Profile() {}

    Profile(String fullName) {
        this.fullName = fullName;
    }

    String getFullName() { return fullName; }
    void setFullName(String fullName) { this.fullName = fullName; }
    String getHeadline() { return headline; }
    void setHeadline(String headline) { this.headline = headline; }
    String getBio() { return bio; }
    void setBio(String bio) { this.bio = bio; }
    String getAvatarUrl() { return avatarUrl; }
    void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    String getLocation() { return location; }
    void setLocation(String location) { this.location = location; }
    String getContactEmail() { return contactEmail; }
    void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    String getAvailability() { return availability; }
    void setAvailability(String availability) { this.availability = availability; }
    String getGithubUrl() { return githubUrl; }
    void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    String getLinkedinUrl() { return linkedinUrl; }
    void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    String getWebsiteUrl() { return websiteUrl; }
    void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    String getResumeUrl() { return resumeUrl; }
    void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }
    List<String> getSkills() { return skills; }
}
