package com.iht.chat.client;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * document-service를 호출해 문서 전체 텍스트를 가져오는 클라이언트.
 *
 * [서비스 간 통신 방식]
 * 지금 단계는 Eureka 같은 서비스 디스커버리 없이, application.yml에 적어둔
 * URL로 직접 RestClient(동기, 논블로킹 아님)를 사용해 호출한다.
 * 서비스가 늘어나거나 동적 스케일링이 필요해지면 그때 디스커버리 도입을 검토한다.
 */
@Component
public class DocumentServiceClient {

    private final RestClient restClient;

    /**
     * IN : baseUrl - application.yml의 services.document-service.url 값을 주입받는다.
     */
    public DocumentServiceClient(@Value("${services.document-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * documentId로 document-service에서 문서 전체 텍스트를 조회한다.
     * IN  : documentId - 조회할 문서 ID
     * OUT : DocumentContentResponse - 문서 전체 텍스트를 담은 DTO
     * @throws BusinessException document-service가 문서를 찾지 못했거나(404),
     *         네트워크 오류 등으로 호출에 실패한 경우
     */
    public DocumentContentResponse fetchDocument(String documentId) {
        // 주의: RestClient는 응답이 4xx/5xx면 .body() 호출 시점에 기본적으로
        // RestClientException(의 하위 클래스)을 던진다. 즉 document-service가 404를 내려줘도
        // 여기서는 정상 응답 대신 예외로 받게 되므로 try-catch로 감싸 우리 쪽 예외로 변환한다.
        try {
            ApiResponse<DocumentContentResponse> response = restClient.get()
                    .uri("/api/documents/{id}", documentId)
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
