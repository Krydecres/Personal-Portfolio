package com.portfolio.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Stable paging envelope so the wire format does not depend on Spring Data internals. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
