package com.portfolio.content.internal;

import com.portfolio.common.error.ServiceException;
import com.portfolio.common.security.Caller;
import com.portfolio.common.util.Slugs;
import com.portfolio.common.web.PageResponse;
import com.portfolio.common.web.Paging;
import com.portfolio.content.api.ContentDtos.PostDetail;
import com.portfolio.content.api.ContentDtos.PostRequest;
import com.portfolio.content.api.ContentDtos.PostStatus;
import com.portfolio.content.api.ContentDtos.PostSummary;
import com.portfolio.content.api.PostService;
import com.portfolio.identity.api.UserDirectory;
import com.portfolio.identity.api.UserDirectory.UserSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.portfolio.common.util.Slugs.clean;

@Service
class PostServiceImpl implements PostService {

    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserDirectory userDirectory;
    private final Clock clock;

    PostServiceImpl(PostRepository posts, CommentRepository comments, UserDirectory userDirectory, Clock clock) {
        this.posts = posts;
        this.comments = comments;
        this.userDirectory = userDirectory;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostSummary> listPublished(int page, int size, String tag) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, Post::getPublishedAt));
        String cleanTag = clean(tag);
        Page<Post> result = cleanTag == null
                ? posts.findByStatus(PostStatus.PUBLISHED, pageable)
                : posts.findByStatusAndTag(PostStatus.PUBLISHED, cleanTag.toLowerCase(Locale.ROOT), pageable);
        return summaries(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetail getPublished(String slug) {
        Post post = posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> ServiceException.notFound("Không tìm thấy bài viết"));
        return detail(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> publishedTags() {
        return posts.findTags(PostStatus.PUBLISHED);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<PostSummary> listAll(int page, int size) {
        return summaries(posts.findAll(Paging.of(page, size, Sort.by(Sort.Direction.DESC, Post::getUpdatedAt))));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PostDetail getForEdit(Long id) {
        return detail(load(id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PostDetail create(Caller author, PostRequest request) {
        Post post = new Post(author.userId());
        apply(post, request);
        return detail(posts.save(post));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PostDetail update(Long id, PostRequest request) {
        Post post = load(id);
        apply(post, request);
        return detail(post);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Post post = load(id);
        comments.deleteForPost(post.getId());
        posts.delete(post);
    }

    private void apply(Post post, PostRequest r) {
        post.setTitle(r.title().trim());
        post.setSummary(clean(r.summary()));
        post.setContent(r.content());
        post.setCoverImageUrl(clean(r.coverImageUrl()));
        post.getTags().clear();
        post.getTags().addAll(normalizeTags(r.tags()));
        post.setSlug(uniqueSlug(clean(r.slug()) != null ? r.slug().trim() : Slugs.slugify(r.title()), post.getId()));
        post.changeStatus(r.status(), clock.instant());
    }

    private String uniqueSlug(String base, Long currentId) {
        // "tags" is a reserved path segment (/api/posts/tags)
        String root = base.isBlank() ? "bai-viet" : base.equals("tags") ? "tags-post" : base;
        String candidate = root;
        int n = 2;
        while (currentId == null ? posts.existsBySlug(candidate) : posts.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = root + "-" + n++;
        }
        return candidate;
    }

    private static Set<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String t : tags) {
            String c = clean(t);
            if (c != null) {
                out.add(c.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private Post load(Long id) {
        return posts.findById(id).orElseThrow(() -> ServiceException.notFound("Không tìm thấy bài viết"));
    }

    private PageResponse<PostSummary> summaries(Page<Post> page) {
        Map<Long, UserSummary> authors = userDirectory.findAll(
                page.getContent().stream().map(p -> p.getAuthorId()).collect(Collectors.toSet()));
        return PageResponse.from(page, p -> new PostSummary(
                p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getCoverImageUrl(),
                List.copyOf(p.getTags()), p.getStatus(), p.getPublishedAt(), p.getUpdatedAt(), p.readingMinutes(),
                authors.getOrDefault(p.getAuthorId(), UserSummary.unknown(p.getAuthorId()))));
    }

    private PostDetail detail(Post p) {
        UserSummary author = userDirectory.find(p.getAuthorId()).orElse(UserSummary.unknown(p.getAuthorId()));
        return new PostDetail(p.getId(), p.getTitle(), p.getSlug(), p.getSummary(), p.getContent(),
                p.getCoverImageUrl(), List.copyOf(p.getTags()), p.getStatus(), p.getPublishedAt(),
                p.getCreatedAt(), p.getUpdatedAt(), p.readingMinutes(), author);
    }
}
