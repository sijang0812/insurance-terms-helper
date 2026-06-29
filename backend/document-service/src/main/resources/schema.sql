-- document-service가 기동될 때마다 자동으로 실행된다 (spring.sql.init.mode=always).
-- 전부 IF NOT EXISTS로 감싸뒀기 때문에 여러 번 실행돼도 안전하다(idempotent).

-- pgvector 확장 활성화. Neon은 기본 권한으로 이 명령이 허용된다.
CREATE EXTENSION IF NOT EXISTS vector;

-- 문서 메타정보 (기존에 메모리에만 있던 정보를 이제 DB에 영구 저장한다)
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    file_name TEXT NOT NULL,
    page_count INT NOT NULL,
    char_count INT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL
);

-- 문서를 잘게 쪼갠 청크 + 각 청크의 임베딩 벡터.
-- text-embedding-3-small 모델 기준 1536차원이라 vector(1536)으로 고정했다.
-- 모델을 바꾸면(차원이 다르면) 이 숫자도 같이 바꿔야 한다.
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL
);

-- 검색 시 "이 문서에 속한 청크만" 빠르게 걸러내기 위한 인덱스.
-- 청크 수가 많이 늘어나면(문서 수가 아주 많아지면) embedding 컬럼에
-- HNSW 근사 인덱스를 추가하는 것도 고려할 수 있다. 지금 규모에서는
-- 문서 하나당 청크가 수백 개 수준이라 brute-force 검색으로 충분하다.
CREATE INDEX IF NOT EXISTS document_chunks_document_id_idx ON document_chunks (document_id);
