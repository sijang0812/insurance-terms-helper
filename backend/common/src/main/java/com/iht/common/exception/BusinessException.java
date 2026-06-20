package com.iht.common.exception;

/**
 * 의도적으로 발생시키는 비즈니스 예외의 기반 클래스.
 * "파일이 PDF가 아니다", "문서를 찾을 수 없다" 같은, 예상 가능한 실패 상황에서 던진다.
 * 각 서비스의 GlobalExceptionHandler가 이 예외를 잡아서 ApiResponse.fail(...)로 변환한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * IN : errorCode - 어떤 종류의 실패인지를 나타내는 ErrorCode
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
