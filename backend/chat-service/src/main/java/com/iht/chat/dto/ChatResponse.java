package com.iht.chat.dto;

/**
 * 채팅 질문에 대한 응답 DTO. 프론트엔드는 answer를 채팅창 왼쪽(봇 답변)에 그대로 렌더링한다.
 *
 * answer   : LLM이 생성한 답변 텍스트
 * provider : 실제로 답변을 생성한 LLM (화면에 "Claude가 답변함" 같은 식으로 표시할 때 사용 가능)
 */
public record ChatResponse(
        String answer,
        String provider
) {}
