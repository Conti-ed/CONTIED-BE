package com.contied.global.exception;

/**
 * AI 추천 결과를 DB 곡 데이터와 매핑하지 못할 때 던지는 예외.
 * GlobalExceptionHandler 에서 HTTP 500으로 처리됩니다.
 */
public class AiMappingException extends RuntimeException {
    public AiMappingException(String message) {
        super(message);
    }
}
