package com.ensolution.ems.global.exception;

public record FieldErrorResponse(
    String field,
    String message
) {
}
