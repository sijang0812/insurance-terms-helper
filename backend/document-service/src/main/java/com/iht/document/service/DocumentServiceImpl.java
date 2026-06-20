package com.iht.document.service;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.document.dto.DocumentUploadResponse;
import com.iht.document.parser.PdfTextExtractor;
import com.iht.document.store.DocumentStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DocumentService의 실제 구현체.
 * 처리 순서: 파일 검증 -> PDF 텍스트 추출 -> 인메모리 저장 -> 응답 DTO 생성
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    /** 업로드 허용 최대 용량 (바이트 단위, 20MB) */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final PdfTextExtractor pdfTextExtractor;
    private final DocumentStore documentStore;

    public DocumentServiceImpl(PdfTextExtractor pdfTextExtractor, DocumentStore documentStore) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.documentStore = documentStore;
    }

    @Override
    public DocumentUploadResponse upload(MultipartFile file) {
        validate(file);

        try {
            PdfTextExtractor.ExtractResult result = pdfTextExtractor.extract(file);
            String documentId = UUID.randomUUID().toString();

            documentStore.save(documentId, new DocumentStore.StoredDocument(
                    file.getOriginalFilename(),
                    result.text(),
                    result.pageCount()
            ));

            return new DocumentUploadResponse(
                    documentId,
                    file.getOriginalFilename(),
                    result.pageCount(),
                    result.text().length(),
                    LocalDateTime.now()
            );
        } catch (IOException e) {
            // PDF가 손상되었거나 암호화되어 있는 등, 파싱 자체가 실패한 경우
            throw new BusinessException(ErrorCode.PDF_PARSE_FAILED);
        }
    }

    @Override
    public DocumentContentResponse getContent(String documentId) {
        DocumentStore.StoredDocument stored = documentStore.find(documentId);
        if (stored == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return new DocumentContentResponse(documentId, stored.fileName(), stored.fullText());
    }

    /**
     * 업로드 파일에 대한 사전 검증.
     * - 비어있지 않은지
     * - 확장자가 .pdf 인지
     * - 용량 제한(MAX_FILE_SIZE)을 넘지 않는지
     * IN  : file - 검증할 업로드 파일
     * OUT : 없음 (검증에 실패하면 BusinessException을 던진다)
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }
}
