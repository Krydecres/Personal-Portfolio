package com.portfolio.profile.api;

import com.portfolio.profile.api.ProfileDtos.ExperienceRequest;
import com.portfolio.profile.api.ProfileDtos.ExperienceResponse;
import com.portfolio.profile.api.ProfileDtos.ProfileResponse;
import com.portfolio.profile.api.ProfileDtos.ProjectRequest;
import com.portfolio.profile.api.ProfileDtos.ProjectResponse;
import com.portfolio.profile.api.ProfileDtos.UpdateProfileRequest;

/**
 * Service contract of the profile service. Reads are public; writes are restricted to the portfolio
 * owner (role ADMIN) and that rule is enforced both at the endpoint and here.
 */
public interface ProfileService {

    ProfileResponse getProfile();

    ProfileResponse updateProfile(UpdateProfileRequest request);

    ExperienceResponse addExperience(ExperienceRequest request);

    ExperienceResponse updateExperience(Long id, ExperienceRequest request);

    void deleteExperience(Long id);

    ProjectResponse addProject(ProjectRequest request);

    ProjectResponse updateProject(Long id, ProjectRequest request);

    void deleteProject(Long id);
}
