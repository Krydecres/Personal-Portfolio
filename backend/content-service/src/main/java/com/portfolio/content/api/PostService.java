package com.portfolio.content.api;

import com.portfolio.common.security.Caller;
import com.portfolio.common.web.PageResponse;
import com.portfolio.content.api.ContentDtos.PostDetail;
import com.portfolio.content.api.ContentDtos.PostRequest;
import com.portfolio.content.api.ContentDtos.PostSummary;

import java.util.List;

/** Service contract for blog posts. Visitors only ever see PUBLISHED posts. */
public interface PostService {

    PageResponse<PostSummary> listPublished(int page, int size, String tag);

    PostDetail getPublished(String slug);

    List<String> publishedTags();

    PageResponse<PostSummary> listAll(int page, int size);

    PostDetail getForEdit(Long id);

    PostDetail create(Caller author, PostRequest request);

    PostDetail update(Long id, PostRequest request);

    void delete(Long id);
}
