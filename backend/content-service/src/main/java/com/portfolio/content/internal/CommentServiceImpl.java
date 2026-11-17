package com.portfolio.content.internal;

import com.portfolio.common.error.ServiceException;
import com.portfolio.common.security.Caller;
import com.portfolio.content.api.CommentService;
import com.portfolio.content.api.ContentDtos.CommentRequest;
import com.portfolio.content.api.ContentDtos.CommentResponse;
import com.portfolio.content.api.ContentDtos.PostStatus;
import com.portfolio.identity.api.UserDirectory;
import com.portfolio.identity.api.UserDirectory.UserSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class CommentServiceImpl implements CommentService {

    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserDirectory userDirectory;

    CommentServiceImpl(PostRepository posts, CommentRepository comments, UserDirectory userDirectory) {
        this.posts = posts;
        this.comments = comments;
        this.userDirectory = userDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> list(String postSlug, Caller viewer) {
        Post post = publishedPost(postSlug);
        List<Comment> list = comments.findForPost(post.getId());
        Map<Long, UserSummary> authors = userDirectory.findAll(
                list.stream().map(c -> c.getAuthorId()).collect(Collectors.toSet()));
        return list.stream()
                .map(c -> toDto(c, authors.getOrDefault(c.getAuthorId(), UserSummary.unknown(c.getAuthorId())), viewer))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public CommentResponse add(String postSlug, Caller author, CommentRequest request) {
        Post post = publishedPost(postSlug);
        Comment saved = comments.save(new Comment(post, author.userId(), request.content().trim()));
        UserSummary summary = userDirectory.find(author.userId()).orElse(UserSummary.unknown(author.userId()));
        return toDto(saved, summary, author);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void delete(Long commentId, Caller caller) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> ServiceException.notFound("Không tìm thấy bình luận"));
        // Object-level authorization: being signed in is not enough, you must own the comment (or be the owner).
        if (!caller.isAdmin() && !caller.owns(comment.getAuthorId())) {
            throw ServiceException.forbidden("Bạn chỉ có thể xoá bình luận của mình");
        }
        comments.delete(comment);
    }

    private Post publishedPost(String slug) {
        return posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> ServiceException.notFound("Không tìm thấy bài viết"));
    }

    private static CommentResponse toDto(Comment c, UserSummary author, Caller viewer) {
        boolean canDelete = viewer != null && (viewer.isAdmin() || viewer.owns(c.getAuthorId()));
        return new CommentResponse(c.getId(), c.getContent(), c.getCreatedAt(), author, canDelete);
    }
}
