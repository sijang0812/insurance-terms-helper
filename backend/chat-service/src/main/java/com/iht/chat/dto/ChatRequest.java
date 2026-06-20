package com.iht.chat.dto;

/**
 * 프론트엔드(Vue.js)가 채팅 질문을 보낼 때 사용하는 요청 DTO.
 *
 * documentId : 질문 대상이 되는 문서 ID (업로드 시 발급받은 값)
 * question   : 사용자가 입력한 자연어 질문
 * provider   : 사용할 LLM. "claude" 또는 "openai". null이거나 빈 값이면 기본값(claude)을 사용한다.
 */
public record ChatRequest(
        String documentId,
        String question,
        String provider
) {}
