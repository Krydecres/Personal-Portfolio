package com.portfolio.content.internal;

import com.portfolio.content.api.ContentDtos.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    // Spring Data binds :tag as a JDBC parameter: user input never becomes part of the SQL text.
    @Query("select p from Post p where p.status = :status and :tag member of p.tags")
    Page<Post> findByStatusAndTag(@Param("status") PostStatus status, @Param("tag") String tag, Pageable pageable);

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("select distinct t from Post p join p.tags t where p.status = :status order by t")
    List<String> findTags(@Param("status") PostStatus status);
}

interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c from Comment c where c.post.id = :postId order by c.createdAt asc")
    List<Comment> findForPost(@Param("postId") Long postId);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    int deleteForPost(@Param("postId") Long postId);
}
