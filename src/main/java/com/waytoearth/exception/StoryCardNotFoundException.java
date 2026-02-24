package com.waytoearth.exception;

/**
 * 스토리 카드를 찾을 수 없을 때 발생하는 예외
 */
public class StoryCardNotFoundException extends BaseBusinessException {

    public StoryCardNotFoundException(String message) {
        super(ErrorCode.STORY_CARD_NOT_FOUND, message);
    }

    public StoryCardNotFoundException(Long storyCardId) {
        super(ErrorCode.STORY_CARD_NOT_FOUND, "스토리 카드를 찾을 수 없습니다: " + storyCardId);
    }
}
