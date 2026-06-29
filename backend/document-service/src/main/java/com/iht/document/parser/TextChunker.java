package com.iht.document.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 긴 텍스트(약관 전문, 수십만~100만 글자)를 임베딩하기 적당한 크기의 청크로 나눈다.
 *
 * [왜 청크로 나누는가]
 * 약관 전체를 매 질문마다 LLM에 통째로 보내면 입력 토큰이 너무 커져서 느리고 비싸진다.
 * 대신 미리 청크 단위로 잘라 각각을 임베딩해두고, 질문이 들어오면 그 질문과 가장
 * 유사한 청크 몇 개만 찾아서 LLM에 넘긴다 (RAG: Retrieval-Augmented Generation).
 *
 * [청크 크기와 겹침(overlap)을 둔 이유]
 * 너무 작게 자르면 문맥이 끊겨서 검색 품질이 떨어지고, 너무 크게 자르면 관련 없는
 * 내용까지 LLM에 같이 들어가 노이즈가 늘어난다. 또한 경계에서 문장이 두 청크로
 * 쪼개지면 양쪽 다 불완전한 문맥이 되므로, 청크 끝부분을 다음 청크 시작에 일부
 * 겹치게(overlap) 해서 손실을 줄인다.
 */
@Component
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    /** 청크 하나의 목표 글자 수 */
    private static final int CHUNK_SIZE = 800;
    /** 청크 경계에서 겹치게 할 글자 수 (문맥 단절 완화용) */
    private static final int OVERLAP = 100;
    /** 문장 경계를 찾기 위해 뒤로 거슬러 올라가며 탐색할 최대 범위 */
    private static final int BOUNDARY_SEARCH_RANGE = 100;

    /**
     * 텍스트를 청크 목록으로 분할한다.
     * IN  : text - 분할할 전체 텍스트 (PDF에서 추출한 약관 본문)
     * OUT : List<String> - 분할된 청크 목록 (순서 보장, 빈 청크는 제외)
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        int skipped = 0;

        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);

            // 잘리는 지점이 문장 중간이면, 근처의 마침표/줄바꿈을 찾아 그 위치로 당겨서 자른다.
            if (end < length) {
                int boundary = findSentenceBoundary(text, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                if (isTocChunk(piece)) {
                    skipped++;
                } else {
                    chunks.add(piece);
                }
            }

            if (end >= length) {
                break;
            }
            // 다음 청크는 OVERLAP 만큼 뒤로 물러난 지점부터 시작한다.
            start = Math.max(end - OVERLAP, start + 1);
        }

        if (skipped > 0) {
            log.info("[chunker] 목차 청크 {}개 제외 (전체 {}개 → 저장 {}개)", skipped, chunks.size() + skipped, chunks.size());
        }
        return chunks;
    }

    /**
     * 목차(TOC) 스타일 청크인지 판별한다.
     * 목차는 "특별약관 ············ 126" 형태로 점 나열과 쪽번호만 있어
     * 임베딩 시 단어 밀도가 높지만 실제 약관 정보가 없어 검색 품질을 오염시킨다.
     * IN  : content - 청크 텍스트
     * OUT : true이면 목차 청크 (임베딩 저장 제외 대상)
     */
    private boolean isTocChunk(String content) {
        if (content.contains("·····") || content.contains(".....")) return true;
        long dotCount = content.chars().filter(c -> c == '·' || c == '.').count();
        return content.length() > 0 && (double) dotCount / content.length() > 0.20;
    }

    /**
     * end 지점 근처에서 문장이 끝나는 위치(마침표 뒤, 또는 줄바꿈)를 찾는다.
     * IN  : text  - 전체 텍스트
     *       start - 이번 청크의 시작 위치 (탐색 하한)
     *       end   - 원래 자르려던 위치
     * OUT : int - 문장 경계로 보이는 위치. 못 찾으면 원래 end를 그대로 반환한다.
     */
    private int findSentenceBoundary(String text, int start, int end) {
        int searchFloor = Math.max(start, end - BOUNDARY_SEARCH_RANGE);
        for (int i = end - 1; i > searchFloor; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '.') {
                return i + 1;
            }
        }
        return end;
    }
}
