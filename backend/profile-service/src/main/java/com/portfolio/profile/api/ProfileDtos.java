package com.portfolio.profile.api;

import com.portfolio.common.validation.HttpUrl;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Wire contract of the profile service. */
public final class ProfileDtos {

    private ProfileDtos() {}

    public record ProfileResponse(
            String fullName,
            String headline,
            String bio,
            String avatarUrl,
            String location,
            String contactEmail,
            String availability,
            String githubUrl,
            String linkedinUrl,
            String websiteUrl,
            String resumeUrl,
            List<String> skills,
            List<ExperienceResponse> experiences,
            List<ProjectResponse> projects
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 120) String fullName,
            @Size(max = 200) String headline,
            @Size(max = 10000, message = "Giới thiệu tối đa 10.000 ký tự") String bio,
            @HttpUrl @Size(max = 1024) String avatarUrl,
            @Size(max = 120) String location,
            @Email(message = "Email liên hệ không hợp lệ") @Size(max = 190) String contactEmail,
            @Size(max = 160) String availability,
            @HttpUrl @Size(max = 512) String githubUrl,
            @HttpUrl @Size(max = 512) String linkedinUrl,
            @HttpUrl @Size(max = 512) String websiteUrl,
            @HttpUrl @Size(max = 1024) String resumeUrl,
            @Size(max = 40, message = "Tối đa 40 kỹ năng")
            List<@NotBlank @Size(max = 40) String> skills
    ) {}

    public record ExperienceResponse(
            Long id,
            String role,
            String organization,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            int sortOrder
    ) {}

    public record ExperienceRequest(
            @NotBlank(message = "Vui lòng nhập vị trí") @Size(max = 120) String role,
            @NotBlank(message = "Vui lòng nhập nơi làm việc") @Size(max = 120) String organization,
            @Size(max = 120) String location,
            @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDate startDate,
            LocalDate endDate,
            @Size(max = 4000) String description,
            Integer sortOrder
    ) {}

    public record ProjectResponse(
            Long id,
            String name,
            String summary,
            List<String> techStack,
            String projectUrl,
            String repoUrl,
            String imageUrl,
            boolean featured,
            int sortOrder
    ) {}

    public record ProjectRequest(
            @NotBlank(message = "Vui lòng nhập tên dự án") @Size(max = 120) String name,
            @Size(max = 2000) String summary,
            @Size(max = 20) List<@NotBlank @Size(max = 40) String> techStack,
            @HttpUrl @Size(max = 1024) String projectUrl,
            @HttpUrl @Size(max = 1024) String repoUrl,
            @HttpUrl @Size(max = 1024) String imageUrl,
            Boolean featured,
            Integer sortOrder
    ) {}
}
