package com.iht.document.store;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 문서 메타정보와 청크+임베딩을 Neon(PostgreSQL + pgvector)에 저장/조회하는 리포지토리.
 *
 * [기존 DocumentStore(인메모리)에서 바뀐 점]
 * 서버를 재시작해도 데이터가 사라지지 않고, 인스턴스를 여러 개 띄워도 같은 DB를
 * 보기 때문에 일관성 문제가 없다. README/설계 문서에 적어뒀던 "DB 연동 시 1순위
 * 교체 대상"이 바로 이 클래스다.
 */
@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * SHA-256 해시로 기존 문서를 조회한다. 같은 파일이 이미 업로드된 경우 중복 임베딩을 막기 위해 사용.
     * IN  : fileHash - 파일 바이트의 SHA-256 해시 (64자 hex 문자열)
     * OUT : ExistingDocument - 일치하는 문서가 있으면 반환, 없으면 null
     */
    public ExistingDocument findByFileHash(String fileHash) {
        List<ExistingDocument> rows = jdbcTemplate.query(
                "SELECT id::text, file_name, page_count, char_count, uploaded_at FROM documents WHERE file_hash = ?",
                (rs, rowNum) -> new ExistingDocument(
                        rs.getString("id"),
                        rs.getString("file_name"),
                        rs.getInt("page_count"),
                        rs.getInt("char_count"),
                        rs.getTimestamp("uploaded_at").toLocalDateTime()),
                fileHash);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 문서 메타정보(파일명, 페이지 수, 파일 크기, 해시 등)를 저장한다.
     * IN  : documentId, fileName, pageCount, charCount, uploadedAt, fileSize, fileHash
     * OUT : 없음
     */
    public void saveDocumentMeta(String documentId, String fileName, int pageCount, int charCount,
                                  LocalDateTime uploadedAt, long fileSize, String fileHash) {
        jdbcTemplate.update(
                "INSERT INTO documents (id, file_name, page_count, char_count, uploaded_at, file_size, file_hash) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.fromString(documentId), fileName, pageCount, charCount, Timestamp.valueOf(uploadedAt), fileSize, fileHash);
    }

    /**
     * 청크 목록과 각 청크의 임베딩 벡터를 한 번의 배치(batch)로 저장한다.
     * 청크가 수백 개에 달할 수 있어, 한 건씩 INSERT하지 않고 배치로 묶어 왕복 횟수를 줄인다.
     * IN  : documentId - 청크들이 속한 문서 ID
     *       contents   - 청크 텍스트 목록
     *       embeddings - contents와 같은 순서의 임베딩 벡터 목록
     * OUT : 없음
     */
    public void saveChunks(String documentId, List<String> contents, List<float[]> embeddings) {
        UUID docId = UUID.fromString(documentId);
        String sql = "INSERT INTO document_chunks (document_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setObject(1, docId);
                ps.setInt(2, i);
                ps.setString(3, contents.get(i));
                ps.setObject(4, new PGvector(embeddings.get(i)));
            }

            @Override
            public int getBatchSize() {
                return contents.size();
            }
        });
    }

    /**
     * documentId로 문서 메타정보를 조회한다.
     * IN  : documentId - 조회할 문서 ID
     * OUT : DocumentMeta - 파일명/페이지 수/글자 수. 없으면 null
     */
    public DocumentMeta findDocumentMeta(String documentId) {
        List<DocumentMeta> rows = jdbcTemplate.query(
                "SELECT file_name, page_count, char_count FROM documents WHERE id = ?",
                (rs, rowNum) -> new DocumentMeta(
                        rs.getString("file_name"), rs.getInt("page_count"), rs.getInt("char_count")),
                UUID.fromString(documentId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 질문 벡터와 가장 유사한 청크들을 pgvector의 코사인 거리 연산자(<=>)로 검색한다.
     * 거리가 작을수록(=0에 가까울수록) 더 유사하므로 오름차순 정렬 후 상위 topK개를 가져온다.
     * IN  : documentId      - 검색 대상 문서 ID (다른 문서의 청크는 섞이지 않도록 필터링)
     *       questionVector  - 질문을 임베딩한 벡터
     *       topK            - 가져올 청크 개수
     * OUT : List<ChunkResult> - 유사도 순으로 정렬된 청크 id + 텍스트 + chunk_index 목록
     */
    public List<ChunkResult> findRelevantChunks(String documentId, float[] questionVector, int topK) {
        return jdbcTemplate.query(
                "SELECT id, content, chunk_index, (embedding <=> ?) AS distance FROM document_chunks WHERE document_id = ? ORDER BY distance LIMIT ?",
                (rs, rowNum) -> new ChunkResult(rs.getLong("id"), rs.getString("content"), rs.getDouble("distance"), rs.getInt("chunk_index")),
                new PGvector(questionVector), UUID.fromString(documentId), topK);
    }

    /**
     * 특정 chunk_index 목록에 해당하는 청크들을 순서대로 가져온다. 인접 청크 확장용.
     * IN  : documentId   - 문서 ID
     *       chunkIndices - 가져올 chunk_index 값 목록
     * OUT : List<SimpleChunk> - chunk_index 오름차순으로 정렬된 청크 목록
     */
    public List<SimpleChunk> findByChunkIndices(String documentId, List<Integer> chunkIndices) {
        if (chunkIndices.isEmpty()) return List.of();
        String placeholders = String.join(", ", Collections.nCopies(chunkIndices.size(), "?"));
        String sql = "SELECT id, content, chunk_index FROM document_chunks WHERE document_id = ? AND chunk_index IN ("
                + placeholders + ") ORDER BY chunk_index";
        List<Object> params = new ArrayList<>();
        params.add(UUID.fromString(documentId));
        params.addAll(chunkIndices);
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new SimpleChunk(rs.getLong("id"), rs.getString("content"), rs.getInt("chunk_index")),
                params.toArray());
    }

    /** 문서 메타정보를 담는 record. fileName / pageCount / charCount */
    public record DocumentMeta(String fileName, int pageCount, int charCount) {}

    /** 중복 파일 감지 시 반환하는 기존 문서 정보 */
    public record ExistingDocument(String id, String fileName, int pageCount, int charCount, LocalDateTime uploadedAt) {}

    /** pgvector 검색 결과 한 행. id = document_chunks.id(PK), content = 청크 텍스트, distance = 코사인 거리, chunkIndex = 문서 내 순번 */
    public record ChunkResult(long id, String content, double distance, int chunkIndex) {}

    /** 인접 청크 조회 결과. distance 없이 id/content/chunkIndex만 포함 */
    public record SimpleChunk(long id, String content, int chunkIndex) {}
}
