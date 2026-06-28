package com.iht.document.controller;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.dto.DocumentSearchRequest;
import com.iht.common.response.ApiResponse;
import com.iht.document.dto.DocumentUploadResponse;
import com.iht.document.service.DocumentService;
import com.iht.document.store.UploadProgressStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF 문서 업로드 및 유사도 검색을 담당하는 REST 컨트롤러.
 * API Gateway를 통해 프론트엔드(Vue.js)의 업로드 요청을 받고,
 * chat-service의 검색 요청(/search)을 내부적으로 받는다.
 *
 * [참고] 예전에 있던 "전체 텍스트 조회"용 GET 엔드포인트는 제거했다. 이제 문서를
 * 통째로 들고 있지 않고 청크 단위로 쪼개 저장하기 때문에 의미가 없어졌고,
 * 디버깅이 필요하면 Neon 콘솔에서 SQL로 직접 document_chunks 테이블을 조회하면 된다.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UploadProgressStore progressStore;

    public DocumentController(DocumentService documentService, UploadProgressStore progressStore) {
        this.documentService = documentService;
        this.progressStore = progressStore;
    }

    /**
     * PDF 파일을 업로드 받아 텍스트 추출 -> 청크 분할 -> 임베딩 -> 저장까지 수행한다.
     * IN  : file     - multipart/form-data로 전송된 PDF 파일 ("file" 파트)
     *       uploadId - 진행률 폴링용 식별자 (프론트엔드가 생성한 UUID, 선택 사항)
     * OUT : ApiResponse<DocumentUploadResponse> - documentId 등 업로드 결과
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadId", required = false, defaultValue = "") String uploadId) {
        DocumentUploadResponse response = documentService.upload(file, uploadId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 업로드 진행 상황을 반환한다. 프론트엔드가 1~2초 간격으로 폴링하는 용도.
     * IN  : body - {"uploadId": "..."} JSON 요청 본문
     * OUT : ApiResponse<UploadProgressStore.Progress> - phase/current/total 포함한 진행 상황.
     *       uploadId를 찾을 수 없으면 phase="done"으로 반환한다.
     */
    @PostMapping("/upload-progress")
    public ResponseEntity<ApiResponse<UploadProgressStore.Progress>> getUploadProgress(
            @RequestBody java.util.Map<String, String> body) {
        String uploadId = body.getOrDefault("uploadId", "");
        UploadProgressStore.Progress progress = progressStore.get(uploadId);
        if (progress == null) {
            progress = new UploadProgressStore.Progress("done", 0, 0);
        }
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * 질문과 가장 관련성이 높은 청크들을 검색해서 반환한다.
     * chat-service가 LLM에 넘길 컨텍스트를 가져올 때 호출하는 내부용 API다.
     * IN  : documentId - 경로 변수로 전달되는 문서 ID
     *       request    - question(사용자 질문)을 담은 요청 본문
     * OUT : ApiResponse<DocumentContentResponse> - fullText 필드에 검색된 관련 청크들
     */
    @PostMapping("/{documentId}/search")
    public ResponseEntity<ApiResponse<DocumentContentResponse>> search(
            @PathVariable String documentId,
            @RequestBody DocumentSearchRequest request) {
        DocumentContentResponse response = documentService.search(documentId, request.question());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
