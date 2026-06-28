package com.iht.document.service;

import com.iht.common.dto.DocumentContentResponse;
import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.document.client.EmbeddingClient;
import com.iht.document.dto.DocumentUploadResponse;
import com.iht.document.parser.PdfTextExtractor;
import com.iht.document.parser.TextChunker;
import com.iht.document.store.DocumentRepository;
import com.iht.document.store.UploadProgressStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DocumentService의 실제 구현체.
 *
 * 업로드 처리 순서:
 *   1) 파일 검증
 *   2) PDF 텍스트 추출 (PdfTextExtractor)
 *   3) 문서 메타정보 저장 (DocumentRepository)
 *   4) 텍스트를 청크로 분할 (TextChunker)
 *   5) 청크들을 임베딩 (EmbeddingClient -> llm-gateway-service)
 *   6) 청크 + 임베딩을 일괄 저장 (DocumentRepository)
 *
 * 검색(search) 처리 순서:
 *   1) 문서 존재 확인
 *   2) 질문을 임베딩
 *   3) pgvector로 가장 유사한 청크 TOP_K개 조회
 *   4) 청크들을 이어붙여 하나의 컨텍스트 문자열로 반환
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    /** 업로드 허용 최대 용량 (바이트 단위, 20MB) */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    /** 질문 하나당 가져올 관련 청크 개수 후보. 목차 청크가 섞일 수 있어 여유있게 가져온다. */
    private static final int TOP_K = 10;
    /** 코사인 거리 임계값. 이 값 이상이면 "관련 없음"으로 판단해 컨텍스트에서 제외한다. (0=동일, 2=반대) */
    private static final double DISTANCE_THRESHOLD = 0.7;
    /** 이 글자 수 이하의 짧은 질문은 보험약관 컨텍스트를 붙여 임베딩 품질을 높인다. */
    private static final int SHORT_QUERY_THRESHOLD = 15;

    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;
    private final DocumentRepository documentRepository;
    private final UploadProgressStore progressStore;

    public DocumentServiceImpl(PdfTextExtractor pdfTextExtractor, TextChunker textChunker,
                                EmbeddingClient embeddingClient, DocumentRepository documentRepository,
                                UploadProgressStore progressStore) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.documentRepository = documentRepository;
        this.progressStore = progressStore;
    }

    @Override
    public DocumentUploadResponse upload(MultipartFile file, String uploadId) {
        validate(file);

        try {
            // 중복 체크: 같은 파일이면 임베딩 없이 기존 문서 ID 반환
            String fileHash = computeSha256(file.getBytes());
            DocumentRepository.ExistingDocument existing = documentRepository.findByFileHash(fileHash);
            if (existing != null) {
                log.info("[upload] 중복 파일 감지 (hash={}...) → 기존 문서 ID={} 반환", fileHash.substring(0, 8), existing.id());
                return new DocumentUploadResponse(existing.id(), existing.fileName(), existing.pageCount(), existing.charCount(), existing.uploadedAt());
            }

            long fileSize = file.getSize();
            progressStore.setPhase(uploadId, "parsing");

            log.info("[upload] 1단계: PDF 텍스트 추출 시작");
            PdfTextExtractor.ExtractResult result = pdfTextExtractor.extract(file);
            log.info("[upload] 2단계: 텍스트 추출 완료 - {}자, {}페이지", result.text().length(), result.pageCount());

            String documentId = UUID.randomUUID().toString();
            LocalDateTime uploadedAt = LocalDateTime.now();

            log.info("[upload] 3단계: 문서 메타 저장 시작");
            documentRepository.saveDocumentMeta(
                    documentId, file.getOriginalFilename(), result.pageCount(), result.text().length(), uploadedAt, fileSize, fileHash);
            log.info("[upload] 4단계: 문서 메타 저장 완료");

            log.info("[upload] 5단계: 청크 분할 시작");
            List<String> chunks = textChunker.chunk(result.text());
            log.info("[upload] 6단계: 청크 분할 완료 - {}개", chunks.size());

            log.info("[upload] 7단계: 임베딩 요청 시작");
            List<float[]> embeddings = embeddingClient.embed(chunks, uploadId);
            log.info("[upload] 8단계: 임베딩 완료 - {}개", embeddings.size());

            progressStore.setPhase(uploadId, "saving");
            log.info("[upload] 9단계: 청크 저장 시작");
            documentRepository.saveChunks(documentId, chunks, embeddings);
            log.info("[upload] 10단계: 청크 저장 완료");
            progressStore.remove(uploadId);

            return new DocumentUploadResponse(
                    documentId,
                    file.getOriginalFilename(),
                    result.pageCount(),
                    result.text().length(),
                    uploadedAt
            );
        } catch (IOException e) {
            progressStore.remove(uploadId);
            log.error("[upload] PDF 파싱 실패", e);
            throw new BusinessException(ErrorCode.PDF_PARSE_FAILED);
        }
    }

    @Override
    public DocumentContentResponse search(String documentId, String question) {
        DocumentRepository.DocumentMeta meta = documentRepository.findDocumentMeta(documentId);
        if (meta == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 짧은 단어 쿼리는 컨텍스트를 보강해 임베딩 품질을 높인다 ("뇌졸중" → "보험약관에서 뇌졸중에 관한 내용")
        String embeddingQuery = question.length() <= SHORT_QUERY_THRESHOLD
                ? "보험약관에서 " + question + "에 관한 내용"
                : question;
        log.info("[search] 질문=\"{}\" → 임베딩쿼리=\"{}\"", question, embeddingQuery);

        float[] questionVector = embeddingClient.embed(List.of(embeddingQuery)).get(0);
        List<DocumentRepository.ChunkResult> candidates = documentRepository.findRelevantChunks(documentId, questionVector, TOP_K);

        // 거리 진단 로그 (검색 품질 확인용)
        candidates.forEach(c -> log.info("[search] 후보 청크 id={} distance={} 미리보기=\"{}\"",
                c.id(), String.format("%.4f", c.distance()),
                c.content().substring(0, Math.min(50, c.content().length()))));

        // 목차 청크 제거 + 임계값 필터. 목차는 관련 단어가 밀집해 similarity가 높지만 실제 정보가 없음.
        List<DocumentRepository.ChunkResult> chunks = candidates.stream()
                .filter(c -> !isTocChunk(c.content()))
                .filter(c -> c.distance() < DISTANCE_THRESHOLD)
                .toList();
        if (chunks.isEmpty()) {
            // fallback: 목차 제외 후 거리순 1위
            chunks = candidates.stream()
                    .filter(c -> !isTocChunk(c.content()))
                    .limit(1)
                    .toList();
        }
        if (chunks.isEmpty() && !candidates.isEmpty()) {
            // 목차만 있는 극단적 상황 — 어쩔 수 없이 목차 포함
            log.warn("[search] 모든 후보가 목차 청크 - fallback으로 최근접 1개 사용");
            chunks = List.of(candidates.get(0));
        }
        log.info("[search] 최종 사용 청크 {}개 (후보 {}개, 목차 제외 후)", chunks.size(), candidates.size());

        // 유사도로 찾은 청크의 앞뒤 ±2 청크를 함께 가져온다.
        // 예: "진단비를 지급합니다" 조항과 실제 금액("3,000만원")이 서로 다른 청크에 있을 때 커버.
        Set<Long> usedIds = new HashSet<>();
        List<Integer> adjacentIndices = new ArrayList<>();
        for (DocumentRepository.ChunkResult c : chunks) {
            usedIds.add(c.id());
            for (int delta = -2; delta <= 2; delta++) {
                if (delta == 0) continue;
                int idx = c.chunkIndex() + delta;
                if (idx >= 0) adjacentIndices.add(idx);
            }
        }
        List<DocumentRepository.SimpleChunk> adjacent = documentRepository.findByChunkIndices(documentId, adjacentIndices);

        // chunk_index 순서로 정렬해 문서 흐름에 맞는 컨텍스트를 구성한다
        Map<Integer, String> contextMap = new LinkedHashMap<>();
        chunks.forEach(c -> contextMap.put(c.chunkIndex(), c.content()));
        adjacent.forEach(a -> contextMap.putIfAbsent(a.chunkIndex(), a.content()));

        List<Long> chunkIds = new ArrayList<>();
        chunks.forEach(c -> chunkIds.add(c.id()));
        adjacent.stream().filter(a -> !usedIds.contains(a.id())).forEach(a -> chunkIds.add(a.id()));

        String context = contextMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.joining("\n\n---\n\n"));

        int totalChunks = contextMap.size();
        log.info("[search] 인접 청크 포함 최종 {}개 (주요 {}개 + 인접 {}개)",
                totalChunks, chunks.size(), totalChunks - chunks.size());
        // 진단 로그: LLM에 실제로 전달되는 청크 내용 확인용
        int[] idx = {0};
        contextMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            String preview = e.getValue().replace("\n", " ").substring(0, Math.min(80, e.getValue().length()));
            log.info("[search] 컨텍스트[{}] chunkIndex={} \"{}\"", ++idx[0], e.getKey(), preview);
        });
        return new DocumentContentResponse(documentId, meta.fileName(), context, chunkIds);
    }

    /**
     * 업로드 파일에 대한 사전 검증.
     * IN  : file - 검증할 업로드 파일
     * OUT : 없음 (검증에 실패하면 BusinessException을 던진다)
     */
    /**
     * 파일 바이트 배열의 SHA-256 해시를 64자 hex 문자열로 반환한다.
     * IN  : bytes - 해시할 바이트 배열
     * OUT : String - 64자 소문자 hex 문자열
     */
    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없는 환경입니다", e);
        }
    }

    /**
     * 목차(TOC) 스타일 청크인지 판별한다.
     * 목차 페이지는 "특별약관 ·············· 126" 처럼 점 나열과 쪽번호만 있어
     * 단어 밀집도는 높지만 실제 보험금 정보가 없다.
     * IN  : content - 청크 텍스트
     * OUT : true이면 목차 청크 (검색 결과에서 제외 대상)
     */
    private boolean isTocChunk(String content) {
        // 연속 중간점(·) 5개 이상이면 목차 리더 패턴으로 판단
        if (content.contains("·····")) return true;
        // 연속 마침표 5개 이상도 목차 형태
        if (content.contains(".....")) return true;
        // 전체 길이 대비 점 문자 비율이 20% 이상이면 목차
        long dotCount = content.chars().filter(c -> c == '·' || c == '.').count();
        return content.length() > 0 && (double) dotCount / content.length() > 0.20;
    }

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
