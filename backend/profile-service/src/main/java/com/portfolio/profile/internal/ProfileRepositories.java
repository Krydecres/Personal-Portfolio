package com.portfolio.profile.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findFirstByOrderByIdAsc();
}

interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findAllByOrderBySortOrderAscStartDateDesc();
}

interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByFeaturedDescSortOrderAscIdDesc();
}
