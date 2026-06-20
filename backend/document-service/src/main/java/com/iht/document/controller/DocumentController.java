package com.iht.document.controller;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.response.ApiResponse;
import com.iht.document.dto.DocumentUploadResponse;
import com.iht.document.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF 문서 업로드 및 조회를 담당하는 REST 컨트롤러.
 * API Gateway를 통해 프론트엔드(Vue.js)의 업로드 요청을 받고,
 * chat-service의 내부 조회 요청도 함께 받는다.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * PDF 파일을 업로드 받아 텍스트를 추출하고 documentId를 발급한다.
     * IN  : file - multipart/form-data로 전송된 PDF 파일 ("file" 파트)
     * OUT : ApiResponse<DocumentUploadResponse> - documentId 등 업로드 결과
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(
            @RequestParam("file") MultipartFile file) {
        DocumentUploadResponse response = documentService.upload(file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * documentId로 저장된 문서의 전체 텍스트를 조회한다.
     * 주로 chat-service가 LLM에 넘길 약관 본문을 가져올 때 호출하는 내부용 API다.
     * IN  : documentId - 경로 변수로 전달되는 문서 ID
     * OUT : ApiResponse<DocumentContentResponse> - 문서 전체 텍스트
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentContentResponse>> getContent(
            @PathVariable String documentId) {
        DocumentContentResponse response = documentService.getContent(documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
