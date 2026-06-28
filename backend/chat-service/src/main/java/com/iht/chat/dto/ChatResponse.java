package com.iht.chat.dto;

import java.util.List;

/**
 * 채팅 질문에 대한 응답 DTO. 프론트엔드는 answer를 채팅창 왼쪽(봇 답변)에 그대로 렌더링한다.
 *
 * answer    : LLM이 생성한 답변 텍스트
 * provider  : 실제로 답변을 생성한 LLM
 * chunkIds  : RAG 검색으로 선택된 document_chunks.id 목록 (디버깅용)
 */
public record ChatResponse(
        String answer,
        String provider,
        List<Long> chunkIds
) {}
