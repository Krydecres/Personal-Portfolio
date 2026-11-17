package com.portfolio.profile.web;

import com.portfolio.profile.api.ProfileDtos.ExperienceRequest;
import com.portfolio.profile.api.ProfileDtos.ExperienceResponse;
import com.portfolio.profile.api.ProfileDtos.ProfileResponse;
import com.portfolio.profile.api.ProfileDtos.ProjectRequest;
import com.portfolio.profile.api.ProfileDtos.ProjectResponse;
import com.portfolio.profile.api.ProfileDtos.UpdateProfileRequest;
import com.portfolio.profile.api.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ProfileResponse profile() {
        return profileService.getProfile();
    }

    @PutMapping("/admin/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(request);
    }

    @PostMapping("/admin/profile/experiences")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceResponse addExperience(@Valid @RequestBody ExperienceRequest request) {
        return profileService.addExperience(request);
    }

    @PutMapping("/admin/profile/experiences/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ExperienceResponse updateExperience(@PathVariable Long id, @Valid @RequestBody ExperienceRequest request) {
        return profileService.updateExperience(id, request);
    }

    @DeleteMapping("/admin/profile/experiences/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExperience(@PathVariable Long id) {
        profileService.deleteExperience(id);
    }

    @PostMapping("/admin/profile/projects")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse addProject(@Valid @RequestBody ProjectRequest request) {
        return profileService.addProject(request);
    }

    @PutMapping("/admin/profile/projects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return profileService.updateProject(id, request);
    }

    @DeleteMapping("/admin/profile/projects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        profileService.deleteProject(id);
    }
}
