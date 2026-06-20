package com.iht.document.exception;

import com.iht.common.exception.BusinessException;
import com.iht.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * document-service 전역 예외 처리기.
 * 컨트롤러 어디서 예외가 발생하든 이 클래스가 가로채서 공통 ApiResponse 포맷으로 변환한다.
 * 덕분에 컨트롤러/서비스 코드에는 try-catch가 거의 없다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 의도적으로 던진 예외(BusinessException)를 처리한다.
     * IN  : e - 발생한 BusinessException (어떤 ErrorCode인지 포함)
     * OUT : ApiResponse 형식의 에러 응답. HTTP 상태 코드는 ErrorCode에 정의된 값을 사용한다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
    }

    /**
     * application.yml에 설정한 업로드 용량 제한(20MB)을 Spring 자체에서 먼저 막았을 때 발생하는 예외.
     * IN  : e - MaxUploadSizeExceededException
     * OUT : 413(Payload Too Large) 상태와 함께 공통 에러 응답을 반환
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail("DOC_413", "파일 용량이 너무 큽니다. (최대 20MB)"));
    }

    /**
     * 위에서 처리하지 못한 모든 예외를 최종적으로 처리한다.
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
