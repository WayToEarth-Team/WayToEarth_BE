package com.waytoearth.exception;

/**
 * 러닝 세션을 찾을 수 없을 때 예외
 */
public class RunningSessionNotFoundException extends BaseBusinessException {

    public RunningSessionNotFoundException(String message) {
        super(ErrorCode.RUNNING_SESSION_NOT_FOUND, message);
    }

    public RunningSessionNotFoundException(String sessionId, String details) {
        super(ErrorCode.RUNNING_SESSION_NOT_FOUND, "러닝 세션을 찾을 수 없습니다. sessionId: " + sessionId + " - " + details);
    }
}
