package com.waytoearth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"),
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "CREW_NOT_FOUND"),
    STORY_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "STORY_CARD_NOT_FOUND"),
    LANDMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "LANDMARK_NOT_FOUND"),
    RUNNING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RUNNING_RECORD_NOT_FOUND"),
    RUNNING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "RUNNING_SESSION_NOT_FOUND"),

    // 400 Bad Request
    DUPLICATE_RESOURCE(HttpStatus.BAD_REQUEST, "DUPLICATE_RESOURCE"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER"),
    CREW_OWNER_CANNOT_DELETE_ACCOUNT(HttpStatus.BAD_REQUEST, "CREW_OWNER_CANNOT_DELETE_ACCOUNT"),
    CREW_ALREADY_OWNED(HttpStatus.BAD_REQUEST, "CREW_ALREADY_OWNED"),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN"),

    // 503 Service Unavailable
    OPENAI_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "OPENAI_SERVICE_ERROR");

    private final HttpStatus status;
    private final String code;
}
