package com.iht.chat.client;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.dto.DocumentSearchRequest;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * document-service를 호출해 "질문과 관련된 약관 청크"를 가져오는 클라이언트.
 *
 * [pgvector 도입 후 바뀐 점]
 * 예전에는 documentId만 주면 전체 텍스트를 받아왔지만, 이제는 약관이 100만 자에 달할
 * 수 있어 전체를 매번 받아오지 않는다. 대신 질문(question)도 함께 보내서, document-service가
 * 내부적으로 질문을 임베딩하고 pgvector로 가장 관련성 높은 청크 몇 개만 찾아 돌려준다.
 */
@Component
public class DocumentServiceClient {

    private final RestClient restClient;

    /** IN : baseUrl - application.yml의 services.document-service.url 값을 주입받는다. */
    public DocumentServiceClient(@Value("${services.document-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * documentId와 question으로 document-service에서 관련 청크들을 검색해 받아온다.
     * IN  : documentId - 검색 대상 문서 ID
     *       question   - 사용자 질문 (document-service가 이걸 임베딩해서 유사도 검색에 사용)
     * OUT : DocumentContentResponse - fullText 필드에 검색된 관련 청크들이 이어붙어 담긴다
     * @throws BusinessException document-service가 문서를 찾지 못했거나(404),
     *         네트워크 오류 등으로 호출에 실패한 경우
     */
    public DocumentContentResponse fetchRelevantContext(String documentId, String question) {
        try {
            ApiResponse<DocumentContentResponse> response = restClient.post()
                    .uri("/api/documents/{id}/search", documentId)
                    .body(new DocumentSearchRequest(question))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<DocumentContentResponse>>() {});

            if (response == null || !response.success()) {
                throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
            }
            return response.data();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
    }
}
