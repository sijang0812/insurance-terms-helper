package com.iht.document.client;

import com.iht.common.dto.EmbeddingRequest;
import com.iht.common.dto.EmbeddingResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.common.response.ApiResponse;
import com.iht.document.store.UploadProgressStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * llm-gateway-service의 임베딩 API(/api/llm/embeddings)를 호출하는 클라이언트.
 * 문서 업로드 시 청크들을 임베딩할 때, 그리고 질문 검색 시 질문 1개를 임베딩할 때
 * 모두 이 클라이언트를 거친다.
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    /**
     * 한 번의 API 호출에 담을 최대 텍스트 개수.
     * 청크가 수백 개에 달할 수 있어, 한 번에 다 보내지 않고 나눠서(batch) 호출한다.
     */
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 3000;

    private final RestClient restClient;
    private final UploadProgressStore progressStore;

    public EmbeddingClient(
            @Value("${services.llm-gateway-service.url}") String baseUrl,
            UploadProgressStore progressStore) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.progressStore = progressStore;
    }

    /**
     * 텍스트 목록을 임베딩 벡터 목록으로 변환한다. 입력 순서와 출력 순서는 항상 동일하다.
     * 질문 1개 임베딩처럼 진행률 추적이 필요 없을 때 사용하는 오버로드.
     * IN  : texts - 임베딩할 텍스트 목록
     * OUT : List<float[]> - 각 텍스트에 대응하는 임베딩 벡터
     */
    public List<float[]> embed(List<String> texts) {
        return embed(texts, null);
    }

    /**
     * 텍스트 목록을 임베딩 벡터 목록으로 변환한다. uploadId가 있으면 배치마다 진행 상황을 기록한다.
     * IN  : texts    - 임베딩할 텍스트 목록 (청크들, 또는 질문 1개짜리 리스트)
     *       uploadId - 진행률 추적용 업로드 식별자 (null이면 추적 안 함)
     * OUT : List<float[]> - 각 텍스트에 대응하는 임베딩 벡터
     */
    public List<float[]> embed(List<String> texts, String uploadId) {
        List<float[]> results = new ArrayList<>();
        int totalBatches = (int) Math.ceil((double) texts.size() / BATCH_SIZE);
        progressStore.setBatchProgress(uploadId, 0, totalBatches);
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int batchNum = i / BATCH_SIZE + 1;
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            log.info("[embedding] 배치 {}/{} 요청 ({}개)", batchNum, totalBatches, batch.size());
            results.addAll(callEmbeddingApi(batch));
            progressStore.setBatchProgress(uploadId, batchNum, totalBatches);
            log.info("[embedding] 배치 {}/{} 완료", batchNum, totalBatches);
        }
        return results;
    }

    private List<float[]> callEmbeddingApi(List<String> batch) {
        RestClientException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ApiResponse<EmbeddingResponse> response = restClient.post()
                        .uri("/api/llm/embeddings")
                        .body(new EmbeddingRequest(batch))
                        .retrieve()
                        .body(new ParameterizedTypeReference<ApiResponse<EmbeddingResponse>>() {});

                if (response == null || !response.success()) {
                    log.error("[embedding] 응답 실패: response={}", response);
                    throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
                }
                return response.data().embeddings();
            } catch (RestClientException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    log.warn("[embedding] 시도 {}/{} 실패, {}ms 후 재시도: {}", attempt, MAX_RETRIES, RETRY_DELAY_MS, e.getMessage());
                    try { TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        log.error("[embedding] {}회 재시도 후 최종 실패: {}", MAX_RETRIES, lastException.getMessage(), lastException);
        throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
    }
}
