package com.iht.common.dto;

import java.util.List;

/**
 * document-service가 llm-gateway-service에 임베딩을 요청할 때 보내는 요청 본문.
 * texts : 임베딩으로 변환할 텍스트 목록 (문서 업로드 시에는 청크들, 질문 검색 시에는 질문 1개)
 */
public record EmbeddingRequest(
        List<String> texts
) {}
