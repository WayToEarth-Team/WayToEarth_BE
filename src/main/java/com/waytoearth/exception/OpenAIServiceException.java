package com.waytoearth.exception;

/**
 * OpenAI API 호출 관련 예외
 * 사용자에게는 친화적인 메시지를 노출하고, 내부 오류는 details에 담는다.
 */
public class OpenAIServiceException extends BaseBusinessException {

    private static final String USER_MESSAGE = "AI 분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

    public OpenAIServiceException(String internalMessage) {
        super(ErrorCode.OPENAI_SERVICE_ERROR, USER_MESSAGE, internalMessage);
    }

    public OpenAIServiceException(String internalMessage, Throwable cause) {
        super(ErrorCode.OPENAI_SERVICE_ERROR, USER_MESSAGE, cause);
    }
}
