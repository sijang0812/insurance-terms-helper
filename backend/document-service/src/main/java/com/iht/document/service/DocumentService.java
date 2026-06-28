package com.iht.document.service;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.document.dto.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 문서(PDF) 처리 관련 비즈니스 로직 인터페이스.
 * Controller는 구현체가 아니라 이 인터페이스에만 의존한다.
 */
public interface DocumentService {

    /**
     * 업로드된 PDF 파일을 검증하고, 텍스트 추출 -> 청크 분할 -> 임베딩 -> DB 저장까지 수행한다.
     * IN  : file     - 사용자가 업로드한 PDF 파일
     *       uploadId - 진행률 폴링용 식별자 (프론트엔드가 생성한 UUID, 빈 문자열이면 추적 안 함)
     * OUT : DocumentUploadResponse - 생성된 documentId 등 업로드 결과 정보
     */
    DocumentUploadResponse upload(MultipartFile file, String uploadId);

    /**
     * 질문과 가장 관련성이 높은 청크들을 pgvector로 검색해 반환한다.
     * chat-service가 LLM에 넘길 컨텍스트를 가져올 때 호출한다.
     * IN  : documentId - 검색 대상 문서 ID
     *       question   - 사용자 질문 (이 질문을 임베딩해서 유사도 검색에 사용한다)
     * OUT : DocumentContentResponse - fullText 필드에 검색된 관련 청크들을 이어붙인 텍스트가 담긴다
     */
    DocumentContentResponse search(String documentId, String question);
}
