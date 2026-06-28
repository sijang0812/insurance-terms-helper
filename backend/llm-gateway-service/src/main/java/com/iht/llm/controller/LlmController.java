package com.iht.llm.controller;

import com.iht.common.dto.ChatTurn;
import com.iht.common.dto.DocumentSearchRequest;
import com.iht.common.dto.LlmChatRequest;
import com.iht.common.dto.LlmChatResponse;
import com.iht.common.response.ApiResponse;
import com.iht.llm.adapter.LlmAdapter;
import com.iht.llm.factory.LlmAdapterFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 외부 LLM 호출을 처리하는 REST 컨트롤러.
 * chat-service가 내부적으로만 호출하며, API Gateway에는 이 서비스로의 라우팅을 두지 않는다
 * (프론트엔드가 직접 호출할 일이 없는, 서비스 내부 전용 API이기 때문).
 */
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmAdapterFactory adapterFactory;

    public LlmController(LlmAdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    /**
     * 지정된 provider의 LLM을 호출해 답변을 생성한다.
     * IN  : request - provider("claude"/"openai"), context(약관 본문), question(질문),
     *                 history(이전 대화 이력)를 담은 요청
     * OUT : ApiResponse<LlmChatResponse> - LLM이 생성한 답변과 사용된 provider
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<LlmChatResponse>> chat(@RequestBody LlmChatRequest request) {
        LlmAdapter adapter = adapterFactory.getAdapter(request.provider());
        List<ChatTurn> history = request.history() == null ? List.of() : request.history();
        String answer = adapter.ask(request.context(), request.question(), history);
        return ResponseEntity.ok(ApiResponse.success(new LlmChatResponse(answer, request.provider())));
    }

    /**
     * 사용자 질문을 보험 약관 공식 용어로 확장한다. 확장은 항상 OpenAI(gpt-4o-mini)로 고정.
     * chat-service가 pgvector 검색 전에 호출하며, 실패 시 원본 질문이 그대로 검색에 사용된다.
     * IN  : request - question(원본 질문)만 사용, 나머지 필드는 무시
     * OUT : ApiResponse<LlmChatResponse> - answer 필드에 확장된 검색어
     */
    @PostMapping("/expand-query")
    public ResponseEntity<ApiResponse<LlmChatResponse>> expandQuery(@RequestBody DocumentSearchRequest request) {
        LlmAdapter adapter = adapterFactory.getAdapter("openai");
        String expanded = adapter.expandQuery(request.question());
        return ResponseEntity.ok(ApiResponse.success(new LlmChatResponse(expanded, "openai")));
    }
}
