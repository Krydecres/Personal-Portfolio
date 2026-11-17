package com.portfolio.content.internal;

import com.portfolio.common.domain.BaseEntity;
import com.portfolio.content.api.ContentDtos.PostStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "uk_posts_slug", columnList = "slug", unique = true),
        @Index(name = "ix_posts_status_published", columnList = "status, published_at")
})
class Post extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(length = 500)
    private String summary;

    /** Markdown source. Rendered on the client without raw HTML, so stored content cannot inject scripts. */
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(nullable = false)
    private String content;

    @Column(name = "cover_image_url", length = 1024)
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag", length = 30, nullable = false)
    @BatchSize(size = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    /** Reference to the identity service by id only: no cross-service foreign-key or entity coupling. */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    protected Post() {}

    Post(Long authorId) {
        this.authorId = authorId;
    }

    void changeStatus(PostStatus next, Instant now) {
        if (next == PostStatus.PUBLISHED && publishedAt == null) {
            publishedAt = now;
        }
        status = next;
    }

    int readingMinutes() {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, Math.round(words / 200f));
    }

    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }
    String getSlug() { return slug; }
    void setSlug(String slug) { this.slug = slug; }
    String getSummary() { return summary; }
    void setSummary(String summary) { this.summary = summary; }
    String getContent() { return content; }
    void setContent(String content) { this.content = content; }
    String getCoverImageUrl() { return coverImageUrl; }
    void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    Set<String> getTags() { return tags; }
    PostStatus getStatus() { return status; }
    Instant getPublishedAt() { return publishedAt; }
    Long getAuthorId() { return authorId; }
}
