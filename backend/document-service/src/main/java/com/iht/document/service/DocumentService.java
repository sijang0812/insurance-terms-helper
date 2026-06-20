package com.iht.document.service;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.document.dto.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 문서(PDF) 처리 관련 비즈니스 로직 인터페이스.
 * Controller는 구현체가 아니라 이 인터페이스에만 의존한다.
 * (테스트 시 Mock으로 교체하기 쉽고, 구현이 바뀌어도 Controller는 영향받지 않는다)
 */
public interface DocumentService {

    /**
     * 업로드된 PDF 파일을 검증하고, 텍스트를 추출해 저장소에 보관한다.
     * IN  : file - 사용자가 업로드한 PDF 파일
     * OUT : DocumentUploadResponse - 생성된 documentId 등 업로드 결과 정보
     */
    DocumentUploadResponse upload(MultipartFile file);

    /**
     * documentId로 저장된 문서의 전체 텍스트를 조회한다.
     * 주로 chat-service가 LLM에 넘길 약관 본문을 가져올 때 내부적으로 호출한다.
     * IN  : documentId - 조회할 문서 ID
     * OUT : DocumentContentResponse - 문서 전체 텍스트를 포함한 응답
     */
    DocumentContentResponse getContent(String documentId);
}
