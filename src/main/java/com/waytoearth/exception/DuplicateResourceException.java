package com.waytoearth.exception;

/**
 * 중복된 리소스 예외
 */
public class DuplicateResourceException extends BaseBusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
