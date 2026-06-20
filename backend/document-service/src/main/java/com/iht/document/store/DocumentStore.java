package com.iht.document.store;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 업로드된 문서의 추출 텍스트를 보관하는 저장소.
 *
 * [주의 - 임시 구현, DB 미연동 상태]
 * 지금은 서버 메모리(ConcurrentHashMap)에만 데이터를 들고 있다.
 *  - 서버를 재시작하면 그동안 업로드된 문서가 전부 사라진다.
 *  - document-service 인스턴스를 여러 개 띄우면(스케일 아웃) 인스턴스마다
 *    서로 다른 메모리를 보게 되어 "방금 올린 문서를 못 찾는" 문제가 생길 수 있다.
 *
 * 이후 DB(RDB) 또는 Redis를 붙일 때는 이 클래스의 내부 구현(필드)만 바꾸면 되고,
 * save / find / exists 라는 메서드 시그니처는 그대로 유지할 수 있도록 설계했다.
 * 즉 이 클래스를 호출하는 DocumentServiceImpl은 수정할 필요가 없다.
 */
@Component
public class DocumentStore {

    private final Map<String, StoredDocument> storage = new ConcurrentHashMap<>();

    /**
     * 문서를 저장소에 저장한다.
     * IN  : documentId - 문서 고유 ID (UUID 문자열)
     *       document   - 저장할 문서 데이터
     * OUT : 없음
     */
    public void save(String documentId, StoredDocument document) {
        storage.put(documentId, document);
    }

    /**
     * documentId로 저장된 문서를 조회한다.
     * IN  : documentId - 조회할 문서 ID
     * OUT : StoredDocument - 저장된 문서 데이터. 없으면 null
     */
    public StoredDocument find(String documentId) {
        return storage.get(documentId);
    }

    /**
     * 저장된 문서 정보를 표현하는 record.
     * fileName  : 원본 파일명
     * fullText  : PDF에서 추출한 전체 텍스트
     * pageCount : PDF 총 페이지 수
     */
    public record StoredDocument(String fileName, String fullText, int pageCount) {}
}
