package com.iht.common.dto;

import java.util.List;

/**
 * llm-gateway-service가 임베딩 요청에 대해 돌려주는 응답 본문.
 * embeddings : 입력 texts와 같은 순서의 벡터 목록. 각 벡터는 1536차원 float 배열
 *              (OpenAI text-embedding-3-small 모델 기준)이다.
 */
public record EmbeddingResponse(
        List<float[]> embeddings
) {}
