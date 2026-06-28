package com.iht.llm.exception;

import com.iht.common.exception.BusinessException;
import com.iht.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * llm-gateway-service 전역 예외 처리기.
 * document-service의 GlobalExceptionHandler와 동일한 패턴을 따른다.
 * (서비스마다 똑같이 반복되는 코드라 다소 중복으로 보일 수 있는데,
 *  서비스별로 독립 배포가 가능해야 하는 MSA 특성상 의도적으로 각자 갖고 있게 했다.
 *  중복이 부담되면 추후 공통 스타터 모듈로 추출하는 것도 고려할 수 있다)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 비즈니스 로직에서 의도적으로 던진 예외(BusinessException)를 처리한다.
     * IN  : e - 발생한 BusinessException
     * OUT : ApiResponse 형식의 에러 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("[BusinessException] code={} message={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
    }

    /**
     * 외부 LLM API 호출 중 예상치 못한 오류가 발생한 경우를 포함해, 처리되지 않은 모든 예외를 잡는다.
     * IN  : e - 처리되지 않은 예외
     * OUT : HTTP 500과 함께 공통 에러 응답을 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("COMMON_500", "서버 내부 오류가 발생했습니다."));
    }
}
