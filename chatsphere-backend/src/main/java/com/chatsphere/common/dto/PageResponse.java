package com.chatsphere.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        PaginationMeta pagination
) {
    public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
        return new PageResponse<>(content, PaginationMeta.fromPage(page));
    }

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return new PageResponse<>(page.getContent(), PaginationMeta.fromPage(page));
    }
}
