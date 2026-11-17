package com.portfolio.content.web;

import com.portfolio.common.security.Caller;
import com.portfolio.common.web.PageResponse;
import com.portfolio.content.api.CommentService;
import com.portfolio.content.api.ContentDtos.CommentRequest;
import com.portfolio.content.api.ContentDtos.CommentResponse;
import com.portfolio.content.api.ContentDtos.PostDetail;
import com.portfolio.content.api.ContentDtos.PostRequest;
import com.portfolio.content.api.ContentDtos.PostSummary;
import com.portfolio.content.api.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    // ---- public ---------------------------------------------------------------------------------

    @GetMapping("/posts")
    public PageResponse<PostSummary> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) @Size(max = 30) String tag) {
        return postService.listPublished(page, size, tag);
    }

    @GetMapping("/posts/tags")
    public List<String> tags() {
        return postService.publishedTags();
    }

    @GetMapping("/posts/{slug}")
    public PostDetail get(@PathVariable @Size(max = 160) String slug) {
        return postService.getPublished(slug);
    }

    @GetMapping("/posts/{slug}/comments")
    public List<CommentResponse> comments(@PathVariable @Size(max = 160) String slug, Caller viewer) {
        return commentService.list(slug, viewer);
    }

    // ---- signed-in users ------------------------------------------------------------------------

    @PostMapping("/posts/{slug}/comments")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable @Size(max = 160) String slug, Caller caller,
                                      @Valid @RequestBody CommentRequest request) {
        return commentService.add(slug, caller, request);
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id, Caller caller) {
        commentService.delete(id, caller);
    }

    // ---- owner only -----------------------------------------------------------------------------

    @GetMapping("/admin/posts")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<PostSummary> listAll(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return postService.listAll(page, size);
    }

    @GetMapping("/admin/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PostDetail getForEdit(@PathVariable Long id) {
        return postService.getForEdit(id);
    }

    @PostMapping("/admin/posts")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetail create(Caller caller, @Valid @RequestBody PostRequest request) {
        return postService.create(caller, request);
    }

    @PutMapping("/admin/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PostDetail update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.update(id, request);
    }

    @DeleteMapping("/admin/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}
