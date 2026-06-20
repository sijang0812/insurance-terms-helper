package com.iht.chat.client;

import com.iht.common.dto.LlmChatRequest;
import com.iht.common.dto.LlmChatResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * llm-gateway-service를 호출해 LLM 답변을 받아오는 클라이언트.
 */
@Component
public class LlmGatewayClient {

    private final RestClient restClient;

    /**
     * IN : baseUrl - application.yml의 services.llm-gateway-service.url 값을 주입받는다.
     */
    public LlmGatewayClient(@Value("${services.llm-gateway-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * llm-gateway-service에 약관 본문과 질문을 보내 답변을 받는다.
     * IN  : provider - 사용할 LLM ("claude" / "openai")
     *       context  - 약관 전체 텍스트
     *       question - 사용자 질문
     * OUT : String - LLM이 생성한 답변 텍스트
     * @throws BusinessException llm-gateway-service 호출이 실패한 경우 (LLM_CALL_FAILED)
     */
    public String ask(String provider, String context, String question) {
        try {
            ApiResponse<LlmChatResponse> response = restClient.post()
                    .uri("/api/llm/chat")
                    .body(new LlmChatRequest(provider, context, question))
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
