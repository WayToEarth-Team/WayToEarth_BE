package com.waytoearth.exception;

/**
 * 잘못된 요청 파라미터 예외
 */
public class InvalidParameterException extends BaseBusinessException {

    public InvalidParameterException(String message) {
        super(ErrorCode.INVALID_PARAMETER, message);
    }
}
