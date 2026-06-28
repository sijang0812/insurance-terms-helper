package com.iht.llm.adapter;

import com.iht.common.dto.ChatTurn;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions API를 호출하는 어댑터.
 *
 * Claude와 가장 큰 차이점: Anthropic은 "system"이 별도의 최상위 필드지만,
 * OpenAI는 system 메시지도 messages 배열 안에 role="system"으로 들어간다.
 */
@Component
public class OpenAiAdapter implements LlmAdapter {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 보험 약관을 일반인이 이해하기 쉽게 설명해주는 도우미입니다.
            아래는 사용자의 질문과 관련성이 높다고 판단되어 약관에서 검색된 일부 내용입니다
            (약관 전문이 아니라 발췌본입니다).
            이 내용을 근거로만 답변하세요. 전문 용어가 나오면 풀어서 쉬운 말로 설명하세요.

            [중요] 보험금 지급 금액에 대해 다음 규칙을 따르세요:
            - 약관에서 금액이 "보험가입금액"으로만 표현된 경우: 구체적인 금액은 계약 시 가입자가 설정한 것이라
              약관에 명시되지 않음을 안내하고, 지급 조건·지급 사유·제외 사유를 중심으로 설명하세요.
            - 약관 내용으로 답변하기 어렵다면 "약관에서 해당 내용을 찾을 수 없어요"라고 답하세요.
              단, 관련 내용이 조금이라도 있다면 그 내용을 먼저 설명한 뒤 한계를 안내하세요.

            ---검색된 약관 내용---
            %s
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    /**
     * IN : apiKey - application.yml의 llm.openai.api-key (환경변수 OPENAI_API_KEY에서 주입)
     *      model  - 사용할 OpenAI 모델명 (application.yml의 llm.openai.model)
     */
    public OpenAiAdapter(
            @Value("${llm.openai.api-key:}") String apiKey,
            @Value("${llm.openai.model:gpt-5.5}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(API_URL).build();
    }

    @Override
    public String providerName() {
        return "openai";
    }

    /**
     * 사용자 질문을 보험 약관 공식 용어로 확장한다. gpt-4o-mini로 고정 (비용 최소화).
     * 키가 없거나 호출 실패 시 원본 질문을 그대로 반환해 검색이 중단되지 않게 한다.
     * IN  : question - 원본 질문
     * OUT : String   - 확장된 검색어 (실패 시 원본 반환)
     */
    @Override
    public String expandQuery(String question) {
        if (apiKey == null || apiKey.isBlank()) return question;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "당신은 보험 약관 검색 전문가입니다. "
                + "사용자 질문에 담긴 개념을 보험 약관에 실제로 등장하는 공식 용어로 확장하세요. "
                + "동의어·상위어·관련 전문 용어를 추가하되 원문 의미를 유지하세요. "
                + "검색어만 한 줄로 출력하고 설명·번호·머리말은 일절 붙이지 마세요. "
                + "예) 심혈관 지급금액 → 심혈관질환, 심근경색, 허혈성심장질환, 협심증, 진단비 지급금액"));
        messages.add(Map.of("role", "user", "content", question));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", "gpt-4o-mini"); // 확장은 저렴한 모델로 고정
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 120);

        try {
            OpenAiResponse response = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String expanded = response.choices().get(0).message().content().trim();
                return expanded.isBlank() ? question : expanded;
            }
        } catch (RestClientException e) {
            // 확장 실패 시 원본 질문으로 fallback — 검색이 중단되면 안 됨
        }
        return question;
    }

    @Override
    public String ask(String context, String question, List<ChatTurn> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.LLM_API_KEY_MISSING);
        }

        List<Map<String, String>> messages = buildMessages(context, question, history);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        try {
            OpenAiResponse response = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
            }
            return response.choices().get(0).message().content();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
        }
    }

    /**
     * system 프롬프트(약관 본문 포함) + 이전 대화 이력 + 새 질문을 OpenAI가 요구하는
     * messages 배열 형태로 만든다. OpenAI는 system도 messages 배열의 첫 항목으로 들어간다.
     * IN  : context, question, history
     * OUT : List<Map<String, String>> - [{role, content}, ...] 형태의 메시지 목록
     */
    private List<Map<String, String>> buildMessages(String context, String question, List<ChatTurn> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT_TEMPLATE.formatted(context)));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", question));
        return messages;
    }

    /** OpenAI Chat Completions API 응답 파싱용 record. */
    private record OpenAiResponse(List<Choice> choices) {}

    private record Choice(OpenAiMessage message) {}

    private record OpenAiMessage(String role, String content) {}
}
