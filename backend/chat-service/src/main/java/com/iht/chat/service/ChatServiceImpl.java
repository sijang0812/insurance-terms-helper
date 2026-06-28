package com.iht.chat.service;

import com.iht.chat.client.DocumentServiceClient;
import com.iht.chat.client.LlmGatewayClient;
import com.iht.chat.dto.ChatRequest;
import com.iht.chat.dto.ChatResponse;
import com.iht.common.dto.ChatTurn;
import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 채팅 오케스트레이션 로직의 실제 구현체.
 *
 * 처리 순서:
 *   1) document-service에서 documentId + question으로 관련도 높은 약관 청크들을 검색해온다
 *      (document-service 내부에서 pgvector 유사도 검색이 일어난다).
 *   2) llm-gateway-service에 (검색된 청크 + 질문 + 대화 이력)을 넘겨 답변을 받는다.
 *
 * 이 클래스는 PDF 파싱도, 임베딩/벡터 검색도, LLM 호출도 직접 하지 않는다 -
 * 두 서비스의 결과를 "조합"만 한다. 이렇게 책임을 분리해두면 검색 방식이 바뀌어도,
 * LLM Provider가 바뀌어도 이 클래스는 전혀 수정할 필요가 없다.
 */
@Service
public class ChatServiceImpl implements ChatService {

    /** 요청에 provider가 지정되지 않았을 때 사용할 기본 LLM */
    private static final String DEFAULT_PROVIDER = "openai";
    /** 질문 최대 바이트 수 (UTF-8 기준, 한국어 약 70자) */
    private static final int MAX_QUESTION_BYTES = 220;

    private final DocumentServiceClient documentServiceClient;
    private final LlmGatewayClient llmGatewayClient;

    public ChatServiceImpl(DocumentServiceClient documentServiceClient, LlmGatewayClient llmGatewayClient) {
        this.documentServiceClient = documentServiceClient;
        this.llmGatewayClient = llmGatewayClient;
    }

    @Override
    public ChatResponse answer(ChatRequest request) {
        // 질문 바이트 수 검증 (과금 방지용 이중 체크 - 프론트에서도 막지만 서버에서도 확인)
        if (request.question().getBytes(StandardCharsets.UTF_8).length > MAX_QUESTION_BYTES) {
            throw new BusinessException(ErrorCode.QUESTION_TOO_LONG);
        }

        // 검색어를 보험 전문 용어로 확장해 벡터 검색 품질을 높인다.
        // 확장된 쿼리는 문서 검색에만 쓰이고, LLM 답변 생성엔 원본 질문을 그대로 보낸다.
        String searchQuery = llmGatewayClient.expandQuery(request.question());
        DocumentContentResponse document =
                documentServiceClient.fetchRelevantContext(request.documentId(), searchQuery);
        String provider = resolveProvider(request.provider());
        List<ChatTurn> history = request.history() == null ? List.of() : request.history();

        String answer = llmGatewayClient.ask(provider, document.fullText(), request.question(), history);
        return new ChatResponse(answer, provider, document.chunkIds());
    }

    /**
     * 요청에 provider가 없으면 기본값(claude)을 사용하도록 보정한다.
     * IN  : provider - 요청에 담겨온 provider 값 (null 또는 빈 문자열 가능)
     * OUT : String - 실제로 사용할 provider 값
     */
    private String resolveProvider(String provider) {
        return (provider == null || provider.isBlank()) ? DEFAULT_PROVIDER : provider;
    }
}
