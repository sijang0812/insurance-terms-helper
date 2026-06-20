package com.iht.llm.adapter;

import org.springframework.stereotype.Component;

/**
 * OpenAI API를 호출하는 어댑터.
 *
 * [현재 상태 - 스텁(stub)]
 * ClaudeAdapter와 마찬가지로 아직 실제 API 연동 전이다.
 * 두 어댑터가 똑같은 LlmAdapter 인터페이스를 구현하기 때문에,
 * 호출하는 쪽(LlmAdapterFactory)은 "claude"든 "openai"든 같은 방식으로 다룬다.
 */
@Component
public class OpenAiAdapter implements LlmAdapter {

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public String ask(String context, String question) {
        // TODO(다음 단계): OpenAI Chat Completions API 실제 연동
        return "[OpenAI 어댑터 - 아직 실제 LLM 연동 전 단계입니다] 질문: \"" + question
                + "\" / 전달받은 약관 본문 길이: " + context.length() + "자";
    }
}
