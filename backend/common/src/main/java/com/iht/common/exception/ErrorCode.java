package com.iht.common.exception;

/**
 * 전 서비스가 공유하는 에러 코드 정의.
 * 새로운 에러 케이스가 생기면 여기에 항목만 추가하면 된다.
 *
 * code    : 클라이언트가 분기 처리할 때 사용할 고유 코드
 * message : 사용자에게 그대로 보여줘도 되는 한국어 메시지
 * status  : 응답할 HTTP 상태 코드 (common 모듈은 순수 Java만 쓰도록
 *           스프링의 HttpStatus 대신 int로 직접 들고 있는다)
 */
public enum ErrorCode {

    // 문서(document-service) 관련
    DOCUMENT_NOT_FOUND("DOC_404", "해당 문서를 찾을 수 없습니다. 업로드가 만료되었을 수 있습니다.", 404),
    INVALID_FILE_TYPE("DOC_400", "PDF 파일만 업로드할 수 있습니다.", 400),
    FILE_TOO_LARGE("DOC_413", "파일 용량이 너무 큽니다. (최대 20MB)", 413),
    PDF_PARSE_FAILED("DOC_500", "PDF 텍스트 추출에 실패했습니다. 파일이 손상되었거나 암호화되어 있을 수 있습니다.", 500),

    // LLM(llm-gateway-service) 관련
    LLM_PROVIDER_NOT_SUPPORTED("LLM_400", "지원하지 않는 LLM Provider입니다.", 400),
    LLM_API_KEY_MISSING("LLM_500", "LLM API 키가 설정되지 않았습니다. 환경변수를 확인해주세요.", 500),
    LLM_CALL_FAILED("LLM_502", "LLM 호출 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", 502),

    // 채팅(chat-service) 관련
    QUESTION_TOO_LONG("CHAT_400", "질문이 너무 깁니다. 220바이트(한국어 약 70자) 이내로 입력해주세요.", 400),

    // 공통
    INTERNAL_SERVER_ERROR("COMMON_500", "서버 내부 오류가 발생했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;

    ErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
