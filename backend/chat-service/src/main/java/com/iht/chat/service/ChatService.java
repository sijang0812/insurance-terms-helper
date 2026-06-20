package com.iht.chat.service;

import com.iht.chat.dto.ChatRequest;
import com.iht.chat.dto.ChatResponse;

/**
 * 채팅 오케스트레이션 로직 인터페이스.
 */
public interface ChatService {

    /**
     * 사용자 질문에 대한 답변을 생성한다.
     * IN  : request - documentId, question, provider를 담은 요청
     * OUT : ChatResponse - 생성된 답변
     */
    ChatResponse answer(ChatRequest request);
}
