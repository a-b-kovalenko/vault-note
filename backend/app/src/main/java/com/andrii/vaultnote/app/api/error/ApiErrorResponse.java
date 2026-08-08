package com.andrii.vaultnote.app.api.error;

public record ApiErrorResponse(
    String code,
    String message) {
}
