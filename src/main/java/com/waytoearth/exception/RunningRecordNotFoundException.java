package com.waytoearth.exception;

/**
 * 러닝 기록을 찾을 수 없을 때 예외
 */
public class RunningRecordNotFoundException extends BaseBusinessException {

    public RunningRecordNotFoundException(String message) {
        super(ErrorCode.RUNNING_RECORD_NOT_FOUND, message);
    }

    public RunningRecordNotFoundException(Long recordId) {
        super(ErrorCode.RUNNING_RECORD_NOT_FOUND, "러닝 기록을 찾을 수 없습니다. recordId: " + recordId);
    }
}
