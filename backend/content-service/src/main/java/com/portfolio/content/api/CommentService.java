package com.portfolio.content.api;

import com.portfolio.common.security.Caller;
import com.portfolio.content.api.ContentDtos.CommentRequest;
import com.portfolio.content.api.ContentDtos.CommentResponse;

import java.util.List;

/** Service contract for comments: any signed-in user may comment; authors and the owner may delete. */
public interface CommentService {

    List<CommentResponse> list(String postSlug, Caller viewer);

    CommentResponse add(String postSlug, Caller author, CommentRequest request);

    void delete(Long commentId, Caller caller);
}
