package com.portfolio.profile.internal;

import com.portfolio.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "experiences")
class Experience extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String role;

    @Column(nullable = false, length = 120)
    private String organization;

    @Column(length = 120)
    private String location;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Experience() {}

    Experience(String role, String organization, LocalDate startDate) {
        this.role = role;
        this.organization = organization;
        this.startDate = startDate;
    }

    String getRole() { return role; }
    void setRole(String role) { this.role = role; }
    String getOrganization() { return organization; }
    void setOrganization(String organization) { this.organization = organization; }
    String getLocation() { return location; }
    void setLocation(String location) { this.location = location; }
    LocalDate getStartDate() { return startDate; }
    void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    LocalDate getEndDate() { return endDate; }
    void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    String getDescription() { return description; }
    void setDescription(String description) { this.description = description; }
    int getSortOrder() { return sortOrder; }
    void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
