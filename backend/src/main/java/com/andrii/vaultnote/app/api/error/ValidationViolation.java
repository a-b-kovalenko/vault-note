package com.andrii.vaultnote.app.api.error;

public record ValidationViolation(
    String field,
    String code,
    String message) {
}
