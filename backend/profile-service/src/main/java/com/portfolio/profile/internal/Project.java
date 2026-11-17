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

@Entity
@Table(name = "projects")
class Project extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String summary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_tech", joinColumns = @JoinColumn(name = "project_id"))
    @OrderColumn(name = "position")
    @Column(name = "tech", length = 40, nullable = false)
    private List<String> techStack = new ArrayList<>();

    @Column(name = "project_url", length = 1024)
    private String projectUrl;

    @Column(name = "repo_url", length = 1024)
    private String repoUrl;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Project() {}

    Project(String name) {
        this.name = name;
    }

    String getName() { return name; }
    void setName(String name) { this.name = name; }
    String getSummary() { return summary; }
    void setSummary(String summary) { this.summary = summary; }
    List<String> getTechStack() { return techStack; }
    String getProjectUrl() { return projectUrl; }
    void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }
    String getRepoUrl() { return repoUrl; }
    void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    String getImageUrl() { return imageUrl; }
    void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    boolean isFeatured() { return featured; }
    void setFeatured(boolean featured) { this.featured = featured; }
    int getSortOrder() { return sortOrder; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
