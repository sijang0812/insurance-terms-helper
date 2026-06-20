package com.iht.common.response;

import java.time.LocalDateTime;

/**
 * 모든 마이크로서비스가 공통으로 사용하는 API 응답 포맷.
 * 프론트엔드(Vue.js)는 이 포맷 하나만 알면 어떤 서비스의 응답이든 동일한 방식으로 처리할 수 있다.
 *
 * success      : 처리 성공 여부
 * data         : 성공 시 실제 응답 데이터 (실패 시 null)
 * errorCode    : 실패 시 에러 코드. 예) "DOC_404" (성공 시 null)
 * errorMessage : 실패 시 사용자에게 보여줄 에러 메시지 (성공 시 null)
 * timestamp    : 응답이 생성된 시각
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String errorMessage,
        LocalDateTime timestamp
) {

    /**
     * 성공 응답을 생성한다.
     * IN  : data - 클라이언트에게 내려줄 실제 데이터
     * OUT : ApiResponse<T> - success=true 로 채워진 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, LocalDateTime.now());
    }

    /**
     * 실패 응답을 생성한다.
     * IN  : errorCode    - 에러를 식별하는 코드 (예: "DOC_404")
     *       errorMessage - 사용자에게 보여줄 에러 메시지
     * OUT : ApiResponse<T> - success=false 로 채워진 응답 객체 (data는 null)
     */
    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, null, errorCode, errorMessage, LocalDateTime.now());
    }
}
