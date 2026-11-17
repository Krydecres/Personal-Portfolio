package com.portfolio.profile.internal;

import com.portfolio.common.error.ServiceException;
import com.portfolio.profile.api.ProfileDtos.ExperienceRequest;
import com.portfolio.profile.api.ProfileDtos.ExperienceResponse;
import com.portfolio.profile.api.ProfileDtos.ProfileResponse;
import com.portfolio.profile.api.ProfileDtos.ProjectRequest;
import com.portfolio.profile.api.ProfileDtos.ProjectResponse;
import com.portfolio.profile.api.ProfileDtos.UpdateProfileRequest;
import com.portfolio.profile.api.ProfileService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

import static com.portfolio.common.util.Slugs.clean;

@Service
class ProfileServiceImpl implements ProfileService, ApplicationRunner {

    private final ProfileRepository profiles;
    private final ExperienceRepository experiences;
    private final ProjectRepository projects;

    ProfileServiceImpl(ProfileRepository profiles, ExperienceRepository experiences, ProjectRepository projects) {
        this.profiles = profiles;
        this.experiences = experiences;
        this.projects = projects;
    }

    /** Creates an empty placeholder profile on first start; the owner fills it in from the admin page. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (profiles.count() == 0) {
            Profile p = new Profile("Tên của bạn");
            p.setHeadline("Một câu ngắn mô tả bạn làm gì");
            p.setBio("Đăng nhập bằng tài khoản quản trị và mở **Quản lý** để viết phần giới thiệu.");
            profiles.save(p);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        Profile p = current();
        return new ProfileResponse(
                p.getFullName(), p.getHeadline(), p.getBio(), p.getAvatarUrl(), p.getLocation(),
                p.getContactEmail(), p.getAvailability(), p.getGithubUrl(), p.getLinkedinUrl(),
                p.getWebsiteUrl(), p.getResumeUrl(), List.copyOf(p.getSkills()),
                experiences.findAllByOrderBySortOrderAscStartDateDesc().stream().map(ProfileServiceImpl::toDto).toList(),
                projects.findAllByOrderByFeaturedDescSortOrderAscIdDesc().stream().map(ProfileServiceImpl::toDto).toList());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileResponse updateProfile(UpdateProfileRequest r) {
        Profile p = current();
        p.setFullName(r.fullName().trim());
        p.setHeadline(clean(r.headline()));
        p.setBio(clean(r.bio()));
        p.setAvatarUrl(clean(r.avatarUrl()));
        p.setLocation(clean(r.location()));
        p.setContactEmail(clean(r.contactEmail()));
        p.setAvailability(clean(r.availability()));
        p.setGithubUrl(clean(r.githubUrl()));
        p.setLinkedinUrl(clean(r.linkedinUrl()));
        p.setWebsiteUrl(clean(r.websiteUrl()));
        p.setResumeUrl(clean(r.resumeUrl()));
        p.getSkills().clear();
        p.getSkills().addAll(dedupe(r.skills()));
        return getProfile();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ExperienceResponse addExperience(ExperienceRequest r) {
        Experience e = new Experience(r.role().trim(), r.organization().trim(), r.startDate());
        apply(e, r);
        return toDto(experiences.save(e));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ExperienceResponse updateExperience(Long id, ExperienceRequest r) {
        Experience e = experiences.findById(id).orElseThrow(() -> ServiceException.notFound("Không tìm thấy kinh nghiệm"));
        e.setRole(r.role().trim());
        e.setOrganization(r.organization().trim());
        e.setStartDate(r.startDate());
        apply(e, r);
        return toDto(e);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteExperience(Long id) {
        Experience e = experiences.findById(id).orElseThrow(() -> ServiceException.notFound("Không tìm thấy kinh nghiệm"));
        experiences.delete(e);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse addProject(ProjectRequest r) {
        Project p = new Project(r.name().trim());
        apply(p, r);
        return toDto(projects.save(p));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse updateProject(Long id, ProjectRequest r) {
        Project p = projects.findById(id).orElseThrow(() -> ServiceException.notFound("Không tìm thấy dự án"));
        p.setName(r.name().trim());
        apply(p, r);
        return toDto(p);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProject(Long id) {
        Project p = projects.findById(id).orElseThrow(() -> ServiceException.notFound("Không tìm thấy dự án"));
        projects.delete(p);
    }

    private Profile current() {
        return profiles.findFirstByOrderByIdAsc()
                .orElseThrow(() -> ServiceException.notFound("Chưa có hồ sơ"));
    }

    private static void apply(Experience e, ExperienceRequest r) {
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw ServiceException.badRequest("INVALID_DATES", "Ngày kết thúc phải sau ngày bắt đầu");
        }
        e.setLocation(clean(r.location()));
        e.setEndDate(r.endDate());
        e.setDescription(clean(r.description()));
        e.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
    }

    private static void apply(Project p, ProjectRequest r) {
        p.setSummary(clean(r.summary()));
        p.setProjectUrl(clean(r.projectUrl()));
        p.setRepoUrl(clean(r.repoUrl()));
        p.setImageUrl(clean(r.imageUrl()));
        p.setFeatured(Boolean.TRUE.equals(r.featured()));
        p.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        p.getTechStack().clear();
        p.getTechStack().addAll(dedupe(r.techStack()));
    }

    private static List<String> dedupe(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String v : values) {
            String c = clean(v);
            if (c != null) {
                set.add(c);
            }
        }
        return List.copyOf(set);
    }

    private static ExperienceResponse toDto(Experience e) {
        return new ExperienceResponse(e.getId(), e.getRole(), e.getOrganization(), e.getLocation(),
                e.getStartDate(), e.getEndDate(), e.getDescription(), e.getSortOrder());
    }

    private static ProjectResponse toDto(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getSummary(), List.copyOf(p.getTechStack()),
                p.getProjectUrl(), p.getRepoUrl(), p.getImageUrl(), p.isFeatured(), p.getSortOrder());
    }
}
