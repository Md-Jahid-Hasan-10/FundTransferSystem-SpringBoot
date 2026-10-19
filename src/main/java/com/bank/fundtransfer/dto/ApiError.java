package com.bank.fundtransfer.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/** The JSON body of every error response. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiError of(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}