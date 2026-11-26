package com.chatsphere.message.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorApiResponse<T>(
        boolean success,
        String message,
        List<T> data,
        String nextCursor,
        boolean hasMore
) {
    public static <T> CursorApiResponse<T> of(List<T> data, String nextCursor, boolean hasMore) {
        return new CursorApiResponse<>(true, "Messages retrieved successfully", data, nextCursor, hasMore);
    }
}
