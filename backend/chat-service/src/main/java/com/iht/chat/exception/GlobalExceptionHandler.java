package com.iht.chat.exception;

import com.iht.common.exception.BusinessException;
import com.iht.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * chat-service 전역 예외 처리기. document-service / llm-gateway-service와 동일한 패턴.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 의도적으로 던진 예외(BusinessException)를 처리한다.
     * 주로 document-service 또는 llm-gateway-service 호출이 실패했을 때 여기로 들어온다.
     * IN  : e - 발생한 BusinessException
     * OUT : ApiResponse 형식의 에러 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
    }

    /**
     * 처리되지 않은 모든 예외를 최종적으로 처리한다.
     * IN  : e - 처리되지 않은 예외
     * OUT : HTTP 500과 함께 공통 에러 응답을 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("COMMON_500", "서버 내부 오류가 발생했습니다."));
    }
}
