package com.iht.llm.embedding;

import com.iht.common.dto.EmbeddingRequest;
import com.iht.common.dto.EmbeddingResponse;
import com.iht.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 텍스트를 임베딩 벡터로 변환하는 내부 전용 API.
 * document-service가 (1) 문서 업로드 시 청크들을, (2) 질문 검색 시 질문 1개를
 * 임베딩할 때 호출한다. chat-service의 LlmController(/api/llm/chat)와 마찬가지로
 * 외부(프론트엔드)에는 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/llm")
public class EmbeddingController {

    private final OpenAiEmbeddingClient embeddingClient;

    public EmbeddingController(OpenAiEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    /**
     * 텍스트 목록을 임베딩 벡터 목록으로 변환한다.
     * IN  : request - texts(임베딩할 텍스트 목록)
     * OUT : ApiResponse<EmbeddingResponse> - 각 텍스트에 대응하는 벡터 목록 (입력 순서와 동일)
     */
    @PostMapping("/embeddings")
    public ResponseEntity<ApiResponse<EmbeddingResponse>> embed(@RequestBody EmbeddingRequest request) {
        List<float[]> vectors = embeddingClient.embed(request.texts());
        return ResponseEntity.ok(ApiResponse.success(new EmbeddingResponse(vectors)));
    }
}
