package com.waytoearth.exception;

/**
 * 사용자를 찾을 수 없을 때 예외
 */
public class UserNotFoundException extends BaseBusinessException {

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId);
    }
}
