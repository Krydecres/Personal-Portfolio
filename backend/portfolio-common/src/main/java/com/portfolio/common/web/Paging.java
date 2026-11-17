package com.portfolio.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Clamps client supplied paging values so a single request cannot ask for huge result sets. */
public final class Paging {

    public static final int MAX_PAGE_SIZE = 50;

    private Paging() {}

    public static PageRequest of(int page, int size, Sort sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
