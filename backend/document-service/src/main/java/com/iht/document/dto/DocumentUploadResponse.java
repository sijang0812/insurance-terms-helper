package com.iht.document.dto;

import java.time.LocalDateTime;

/**
 * PDF 업로드 성공 시 프론트엔드에게 내려주는 응답 DTO.
 * 이 DTO는 document-service 전용이며 다른 서비스와 공유하지 않는다.
 * (서비스 간에 공유해야 하는 모양은 common.dto 패키지에 둔다)
 *
 * documentId : 이후 채팅 시 이 문서를 가리키기 위한 고유 ID. 프론트엔드는 이 값을 저장해뒀다가
 *              채팅 요청을 보낼 때 함께 전달해야 한다.
 * fileName   : 업로드한 원본 파일명 (화면에 그대로 표시 가능)
 * pageCount  : PDF 총 페이지 수
 * charCount  : 추출된 텍스트 글자 수 (약관 분량을 가늠하는 용도)
 * uploadedAt : 업로드 처리 시각
 */
public record DocumentUploadResponse(
        String documentId,
        String fileName,
        int pageCount,
        int charCount,
        LocalDateTime uploadedAt
) {}
