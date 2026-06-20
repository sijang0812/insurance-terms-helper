package com.iht.chat.controller;

import com.iht.chat.dto.ChatRequest;
import com.iht.chat.dto.ChatResponse;
import com.iht.chat.service.ChatService;
import com.iht.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 질문을 받아 답변을 생성하는 채팅 REST 컨트롤러.
 * API Gateway를 통해 프론트엔드(Vue.js) 채팅 화면이 호출하는 진입점이다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 약관에 대한 사용자 질문에 답변한다.
     * IN  : request - documentId(대상 문서), question(질문), provider(사용할 LLM, 선택값)
     * OUT : ApiResponse<ChatResponse> - 생성된 답변
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.answer(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
