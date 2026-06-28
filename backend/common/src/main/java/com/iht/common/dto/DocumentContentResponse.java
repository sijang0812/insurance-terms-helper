package com.iht.common.dto;

import java.util.List;

/**
 * 문서 전체 텍스트를 표현하는 서비스 간 공유 계약(contract) DTO.
 * document-service가 응답으로 내려주고, chat-service가 그대로 받아서 사용한다.
 * 두 서비스가 서로 다른 모양의 클래스를 따로 들고 있다가 어긋나는 것을 막기 위해
 * common 모듈에 두고 함께 의존한다.
 *
 * documentId : 문서 고유 ID
 * fileName   : 업로드 당시의 원본 파일명
 * fullText   : pgvector 검색으로 뽑힌 관련 청크들을 이어붙인 텍스트
 * chunkIds   : 선택된 청크들의 document_chunks.id (디버깅용)
 */
public record DocumentContentResponse(
        String documentId,
        String fileName,
        String fullText,
        List<Long> chunkIds
) {}
