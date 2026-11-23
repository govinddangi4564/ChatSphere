package com.chatsphere.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String message,
        List<ValidationError> errors
) {
    public ErrorResponse(String message) {
        this(false, message, null);
    }

    public ErrorResponse(String message, List<ValidationError> errors) {
        this(false, message, errors);
    }

    public record ValidationError(
            String field,
            String message
    ) {}
}
