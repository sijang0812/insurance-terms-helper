package com.iht.common.dto;

/**
 * llm-gateway-service가 LLM 호출 결과로 돌려주는 응답 본문.
 *
 * answer   : LLM이 생성한 답변 텍스트
 * provider : 실제로 답변을 생성한 LLM ("claude" 또는 "openai")
 */
public record LlmChatResponse(
        String answer,
        String provider
) {}
