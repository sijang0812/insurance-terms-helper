package com.iht.llm.adapter;

import org.springframework.stereotype.Component;

/**
 * Anthropic Claude API를 호출하는 어댑터.
 *
 * [현재 상태 - 스텁(stub)]
 * 아직 Anthropic API 키 연동 전이라, 실제 호출 없이 더미 응답만 돌려준다.
 * 전체 파이프라인(Gateway -> Chat -> LLM Gateway)이 끊김 없이 동작하는지
 * 먼저 확인하기 위한 단계다. 다음 단계에서 실제 Anthropic Messages API 호출 로직으로 교체한다.
 */
@Component
public class ClaudeAdapter implements LlmAdapter {

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public String ask(String context, String question) {
        // TODO(다음 단계): Anthropic Messages API 실제 연동
        return "[Claude 어댑터 - 아직 실제 LLM 연동 전 단계입니다] 질문: \"" + question
                + "\" / 전달받은 약관 본문 길이: " + context.length() + "자";
    }
}
