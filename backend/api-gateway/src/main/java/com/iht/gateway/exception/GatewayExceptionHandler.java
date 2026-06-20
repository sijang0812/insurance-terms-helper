package com.iht.gateway.exception;

import com.iht.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/**
 * api-gateway 전역 예외 처리기.
 * 다른 서비스들과 달리 BusinessException은 거의 발생하지 않는다(게이트웨이는 비즈니스 로직이 없으므로).
 * 대신 "다운스트림 서비스가 아예 떠있지 않은 경우"를 사용자 친화적인 메시지로 바꿔주는 역할이 더 중요하다.
 */
@RestControllerAdvice
public class GatewayExceptionHandler {

    /**
     * document-service / chat-service에 연결 자체가 안 되는 경우 (예: 해당 서비스가 아직 안 떠있음).
     * 로컬 개발 중 "분명 코드는 맞는데 502/Connection refused가 난다"면 대부분 이 케이스다.
     * IN  : e - 연결 실패 예외
     * OUT : 502(Bad Gateway)와 함께, 어떤 서비스 문제인지 짐작할 수 있는 메시지를 반환
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAccess(ResourceAccessException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("GATEWAY_502",
                        "내부 서비스에 연결할 수 없습니다. document-service / chat-service가 켜져 있는지 확인해주세요."));
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
