package com.portfolio.identity.api.dto;

import com.portfolio.common.validation.HttpUrl;
import com.portfolio.identity.api.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Wire contract of the identity service. */
public final class IdentityDtos {

    private IdentityDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Vui lòng nhập email")
            @Email(message = "Email không hợp lệ")
            @Size(max = 190, message = "Email quá dài")
            String email,

            @NotBlank(message = "Vui lòng nhập tên hiển thị")
            @Size(min = 2, max = 80, message = "Tên hiển thị dài 2–80 ký tự")
            String displayName,

            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @StrongPassword
            String password,

            @NotBlank(message = "Vui lòng nhập lại mật khẩu")
            String confirmPassword
    ) {
        @Override
        public String toString() {
            return "RegisterRequest[email=" + email + ", displayName=" + displayName + ", password=***]";
        }
    }

    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập email")
            @Size(max = 190)
            String email,

            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @Size(max = 128)
            String password
    ) {
        @Override
        public String toString() {
            return "LoginRequest[email=" + email + ", password=***]";
        }
    }

    public record UpdateAccountRequest(
            @NotBlank(message = "Vui lòng nhập tên hiển thị")
            @Size(min = 2, max = 80, message = "Tên hiển thị dài 2–80 ký tự")
            String displayName,

            @HttpUrl
            @Size(max = 1024)
            String avatarUrl
    ) {}

    public record ChangePasswordRequest(
            @Size(max = 128)
            String currentPassword,

            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @StrongPassword
            String newPassword,

            @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
            String confirmPassword
    ) {
        @Override
        public String toString() {
            return "ChangePasswordRequest[***]";
        }
    }

    public record UserResponse(
            Long id,
            String email,
            String displayName,
            String avatarUrl,
            String role,
            boolean hasPassword,
            boolean googleLinked
    ) {}

    /** Body of a successful login/refresh. The refresh token is never in the body: it travels in an HttpOnly cookie. */
    public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}

    public record PasswordRuleResponse(String id, String label) {}

    public record AuthProvidersResponse(boolean google, List<PasswordRuleResponse> passwordRules) {}

    /** Verified data received from Google after the OIDC flow. */
    public record GoogleProfile(String subject, String email, boolean emailVerified, String name, String pictureUrl) {}
}
