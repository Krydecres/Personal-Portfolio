package com.portfolio.content.internal;

import com.portfolio.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments", indexes = @Index(name = "ix_comments_post", columnList = "post_id, created_at"))
class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 2000)
    private String content;

    protected Comment() {}

    Comment(Post post, Long authorId, String content) {
        this.post = post;
        this.authorId = authorId;
        this.content = content;
    }

    Post getPost() { return post; }
    Long getAuthorId() { return authorId; }
    String getContent() { return content; }
}
