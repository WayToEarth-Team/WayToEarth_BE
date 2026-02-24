package com.waytoearth.exception;

/**
 * 크루를 찾을 수 없는 경우 발생하는 예외
 */
public class CrewNotFoundException extends BaseBusinessException {

    public CrewNotFoundException(Long crewId) {
        super(ErrorCode.CREW_NOT_FOUND, "크루를 찾을 수 없습니다. crewId: " + crewId);
    }

    public CrewNotFoundException(String message) {
        super(ErrorCode.CREW_NOT_FOUND, message);
    }

    public CrewNotFoundException(String message, Throwable cause) {
        super(ErrorCode.CREW_NOT_FOUND, message, cause);
    }
}
