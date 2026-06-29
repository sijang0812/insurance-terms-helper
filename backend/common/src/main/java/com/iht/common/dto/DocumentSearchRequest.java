package com.iht.common.dto;

/**
 * chat-service가 document-service에 "이 문서에서 질문과 관련된 부분을 찾아줘"라고
 * 요청할 때 보내는 요청 본문.
 * question : 사용자가 입력한 질문. document-service가 이 질문을 임베딩한 뒤
 *            pgvector로 가장 유사한 청크들을 찾는 데 사용한다.
 */
public record DocumentSearchRequest(
        String question
) {}
