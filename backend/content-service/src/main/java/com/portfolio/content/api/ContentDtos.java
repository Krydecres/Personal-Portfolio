package com.portfolio.content.api;

import com.portfolio.common.validation.HttpUrl;
import com.portfolio.identity.api.UserDirectory.UserSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Wire contract of the content service. */
public final class ContentDtos {

    private ContentDtos() {}

    public enum PostStatus { DRAFT, PUBLISHED }

    public record PostSummary(
            Long id,
            String title,
            String slug,
            String summary,
            String coverImageUrl,
            List<String> tags,
            PostStatus status,
            Instant publishedAt,
            Instant updatedAt,
            int readingMinutes,
            UserSummary author
    ) {}

    public record PostDetail(
            Long id,
            String title,
            String slug,
            String summary,
            String content,
            String coverImageUrl,
            List<String> tags,
            PostStatus status,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt,
            int readingMinutes,
            UserSummary author
    ) {}

    public record PostRequest(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
            String title,

            @Size(max = 160)
            @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang")
            String slug,

            @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự")
            String summary,

            @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 200_000, message = "Nội dung quá dài")
            String content,

            @HttpUrl @Size(max = 1024)
            String coverImageUrl,

            @Size(max = 10, message = "Tối đa 10 thẻ")
            List<@NotBlank @Size(max = 30) String> tags,

            @NotNull(message = "Vui lòng chọn trạng thái")
            PostStatus status
    ) {}

    public record CommentResponse(Long id, String content, Instant createdAt, UserSummary author, boolean canDelete) {}

    public record CommentRequest(
            @NotBlank(message = "Bình luận không được để trống")
            @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
            String content
    ) {}
}
