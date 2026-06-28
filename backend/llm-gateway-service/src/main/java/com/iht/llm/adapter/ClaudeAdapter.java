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
 * Anthropic Claude API(Messages API)를 호출하는 어댑터.
 *
 * 공식 문서: https://docs.claude.com/en/api/messages-examples
 *
 * [요청 본문을 record가 아니라 Map으로 구성하는 이유]
 * Anthropic API는 "max_tokens"처럼 스네이크 케이스 필드명을 쓰는데, 자바 record로
 * 만들면 @JsonProperty 같은 Jackson 어노테이션으로 필드명을 다시 매핑해줘야 한다.
 * 지금은 Spring Boot 4 / Jackson 3로 올라가면서 일부 어노테이션 패키지가 바뀌었을
 * 가능성이 있어 확신이 서지 않는 부분이라, 그냥 Map으로 직접 JSON 키를 명시하는
 * 더 안전한 방식을 택했다. (빌드 에러 위험을 줄이는 선택)
 */
@Component
public class ClaudeAdapter implements LlmAdapter {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;

    /** 약관 본문을 system 프롬프트에 끼워 넣기 위한 템플릿. %s 자리에 검색된 관련 청크들이 들어간다. */
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
     * IN : apiKey - application.yml의 llm.claude.api-key (환경변수 ANTHROPIC_API_KEY에서 주입)
     *      model  - 사용할 Claude 모델명 (application.yml의 llm.claude.model)
     */
    public ClaudeAdapter(
            @Value("${llm.claude.api-key:}") String apiKey,
            @Value("${llm.claude.model:claude-sonnet-4-6}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(API_URL).build();
    }

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public String ask(String context, String question, List<ChatTurn> history) {
        if (!isUsableKey(apiKey)) {
            return "[Mock 답변] Claude API 키가 없어 실제 답변을 생성하지 않습니다. RAG 청크 ID는 브라우저 콘솔에서 확인하세요.";
        }

        List<Map<String, String>> messages = buildMessages(question, history);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("system", SYSTEM_PROMPT_TEMPLATE.formatted(context));
        requestBody.put("messages", messages);

        try {
            ClaudeResponse response = restClient.post()
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ClaudeResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
            }
            return response.content().get(0).text();
        } catch (RestClientException e) {
            // 네트워크 오류, 401(키 오류), 429(rate limit), 5xx 등을 전부 여기서 받는다.
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED);
        }
    }

    /**
     * API 키가 실제로 사용 가능한 값인지 확인한다.
     * 빈 값이거나 ASCII 범위를 벗어난 문자(한글 placeholder 등)가 있으면 false.
     * IN  : key - 검사할 API 키 문자열
     * OUT : 사용 가능하면 true, 아니면 false
     */
    private boolean isUsableKey(String key) {
        return key != null && !key.isBlank() && key.chars().allMatch(c -> c < 128);
    }

    /**
     * 이전 대화 이력 + 새 질문을 Anthropic Messages API가 요구하는 messages 배열 형태로 만든다.
     * IN  : question - 이번 turn의 질문
     *       history  - 이전 대화 turn 목록
     * OUT : List<Map<String, String>> - [{role, content}, ...] 형태의 메시지 목록
     */
    private List<Map<String, String>> buildMessages(String question, List<ChatTurn> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", question));
        return messages;
    }

    /** Anthropic Messages API 응답 파싱용 record. content[0].text가 실제 답변이다. */
    private record ClaudeResponse(List<ContentBlock> content) {}

    private record ContentBlock(String type, String text) {}
}
