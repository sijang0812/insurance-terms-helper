package com.iht.chat.service;

import com.iht.chat.client.DocumentServiceClient;
import com.iht.chat.client.LlmGatewayClient;
import com.iht.chat.dto.ChatRequest;
import com.iht.chat.dto.ChatResponse;
import com.iht.common.dto.DocumentContentResponse;
import org.springframework.stereotype.Service;

/**
 * 채팅 오케스트레이션 로직의 실제 구현체.
 *
 * 처리 순서:
 *   1) document-service에서 documentId에 해당하는 약관 전체 텍스트를 가져온다.
 *   2) llm-gateway-service에 (약관 본문 + 질문)을 넘겨 답변을 받는다.
 *
 * 이 클래스는 PDF 파싱도, LLM 호출도 직접 하지 않는다 - 두 서비스의 결과를 "조합"만 한다.
 * 이렇게 책임을 분리해두면 PDF 파싱 로직이 바뀌어도, LLM Provider가 바뀌어도
 * 이 클래스는 전혀 수정할 필요가 없다.
 */
@Service
public class ChatServiceImpl implements ChatService {

    /** 요청에 provider가 지정되지 않았을 때 사용할 기본 LLM */
    private static final String DEFAULT_PROVIDER = "claude";

    private final DocumentServiceClient documentServiceClient;
    private final LlmGatewayClient llmGatewayClient;

    public ChatServiceImpl(DocumentServiceClient documentServiceClient, LlmGatewayClient llmGatewayClient) {
        this.documentServiceClient = documentServiceClient;
        this.llmGatewayClient = llmGatewayClient;
    }

    @Override
    public ChatResponse answer(ChatRequest request) {
        DocumentContentResponse document = documentServiceClient.fetchDocument(request.documentId());
        String provider = resolveProvider(request.provider());

        String answer = llmGatewayClient.ask(provider, document.fullText(), request.question());
        return new ChatResponse(answer, provider);
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
