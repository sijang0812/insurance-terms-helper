package com.iht.llm.embedding;

import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * OpenAI Embeddings API를 호출해 텍스트를 벡터로 변환하는 컴포넌트.
 *
 * [왜 임베딩은 항상 OpenAI인가]
 * 채팅 답변 생성(ClaudeAdapter/OpenAiAdapter)은 사용자가 provider로 고를 수 있지만,
 * 임베딩은 그것과 별개의 관심사다. Anthropic은 자체 임베딩 API를 제공하지 않고
 * Voyage AI 같은 외부 파트너를 권장하는데, 이미 OpenAI 키가 있으니 굳이 의존성을
 * 더 늘리지 않고 OpenAI의 text-embedding-3-small(1536차원)을 그대로 쓴다.
 * Claude만 쓰고 싶어도 임베딩을 위해 OpenAI 키는 필요하다는 점을 기억해야 한다.
 */
@Component
public class OpenAiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingClient.class);
    private static final String API_URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";
    private static final int VECTOR_DIM = 1536;
    private static final Random RANDOM = new Random();

    private final RestClient restClient;
    private final String apiKey;

    /** IN : apiKey - llm.openai.api-key (ClaudeAdapter/OpenAiAdapter와 같은 키를 재사용) */
    public OpenAiEmbeddingClient(@Value("${llm.openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl(API_URL).build();
    }

    /**
     * 텍스트 목록을 임베딩 벡터 목록으로 변환한다. 입력 순서와 출력 순서는 항상 동일하다
     * (OpenAI API가 보장하는 동작이다).
     * IN  : texts - 임베딩할 텍스트 목록
     * OUT : List<float[]> - 각 텍스트에 대응하는 1536차원 벡터
     */
    public List<float[]> embed(List<String> texts) {
        // API 키가 없으면 mock 모드: 랜덤 벡터를 반환한다.
        // 실제 유사도 검색은 동작 안 하지만, DB 저장/파이프라인 테스트용으로는 충분하다.
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[OpenAI Embedding] API 키 없음 - mock 벡터 사용 (테스트 전용, 검색 품질 보장 안 됨)");
            return generateMockVectors(texts.size());
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("input", texts);

        try {
            EmbeddingApiResponse response = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(EmbeddingApiResponse.class);

            if (response == null || response.data() == null) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
            }
            return toFloatArrays(response.data());
        } catch (RestClientResponseException e) {
            log.error("[OpenAI Embedding] HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
        } catch (RestClientException e) {
            log.error("[OpenAI Embedding] 네트워크 오류: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
        }
    }

    /** API 키 없을 때 테스트용 랜덤 벡터를 생성한다. */
    private List<float[]> generateMockVectors(int count) {
        List<float[]> vectors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float[] v = new float[VECTOR_DIM];
            for (int j = 0; j < VECTOR_DIM; j++) {
                v[j] = RANDOM.nextFloat() * 2 - 1;
            }
            vectors.add(v);
        }
        return vectors;
    }

    /** OpenAI 응답의 List<Float> 형태를 pgvector가 요구하는 float[]로 변환한다. */
    private List<float[]> toFloatArrays(List<EmbeddingItem> items) {
        List<float[]> vectors = new ArrayList<>();
        for (EmbeddingItem item : items) {
            List<Float> values = item.embedding();
            float[] vector = new float[values.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = values.get(i);
            }
            vectors.add(vector);
        }
        return vectors;
    }

    /** OpenAI Embeddings API 응답 파싱용 record. (object, index 등 안 쓰는 필드는 무시됨) */
    private record EmbeddingApiResponse(List<EmbeddingItem> data) {}

    private record EmbeddingItem(List<Float> embedding) {}
}
