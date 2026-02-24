package com.waytoearth.exception;

/**
 * 인증 관련 예외
 */
public class UnauthorizedException extends BaseBusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
