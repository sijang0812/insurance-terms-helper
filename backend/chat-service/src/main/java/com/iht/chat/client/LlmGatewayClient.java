package com.iht.chat.client;

import com.iht.common.dto.ChatTurn;
import com.iht.common.dto.DocumentSearchRequest;
import com.iht.common.dto.LlmChatRequest;
import com.iht.common.dto.LlmChatResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * llm-gateway-service를 호출해 LLM 답변을 받아오는 클라이언트.
 */
@Component
public class LlmGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(LlmGatewayClient.class);

    private final RestClient restClient;

    /**
     * IN : baseUrl - application.yml의 services.llm-gateway-service.url 값을 주입받는다.
     */
    public LlmGatewayClient(@Value("${services.llm-gateway-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * 사용자 질문을 보험 약관 공식 용어로 확장해 검색 품질을 높인다.
     * 호출 실패 시 원본 질문을 그대로 반환 — 검색이 중단되면 안 되므로 예외를 삼킨다.
     * IN  : question - 원본 사용자 질문
     * OUT : String   - 확장된 검색어 (실패 시 원본 반환)
     */
    public String expandQuery(String question) {
        try {
            ApiResponse<LlmChatResponse> response = restClient.post()
                    .uri("/api/llm/expand-query")
                    .body(new DocumentSearchRequest(question))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<LlmChatResponse>>() {});

            if (response != null && response.success() && response.data() != null) {
                String expanded = response.data().answer().trim();
                log.info("[expandQuery] \"{}\" → \"{}\"", question, expanded);
                return expanded.isBlank() ? question : expanded;
            }
        } catch (RestClientException e) {
            log.warn("[expandQuery] 확장 실패, 원본 질문 사용: {}", e.getMessage());
        }
        return question;
    }

    /**
     * llm-gateway-service에 약관 본문, 질문, 이전 대화 이력을 보내 답변을 받는다.
     * IN  : provider - 사용할 LLM ("claude" / "openai")
     *       context  - 약관 전체 텍스트
     *       question - 사용자 질문 (이번 turn)
     *       history  - 이전까지의 대화 turn 목록
     * OUT : String - LLM이 생성한 답변 텍스트
     * @throws BusinessException llm-gateway-service 호출이 실패한 경우 (LLM_CALL_FAILED)
     */
    public String ask(String provider, String context, String question, List<ChatTurn> history) {
        try {
            ApiResponse<LlmChatResponse> response = restClient.post()
                    .uri("/api/llm/chat")
                    .body(new LlmChatRequest(provider, context, question, history))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<LlmChatResponse>>() {});

            if (response == null || !response.success()) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
            }
            return response.data().answer();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
        }
    }
}
