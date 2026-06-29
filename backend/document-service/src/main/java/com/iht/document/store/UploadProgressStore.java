package com.iht.document.store;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 업로드 진행 상황을 메모리에 저장하는 컴포넌트.
 * 프론트엔드가 폴링으로 진행률을 조회할 수 있도록 uploadId 단위로 상태를 관리한다.
 * 업로드 완료(또는 실패) 후 remove()를 호출해 메모리를 정리해야 한다.
 */
@Component
public class UploadProgressStore {

    private final ConcurrentHashMap<String, Progress> store = new ConcurrentHashMap<>();

    /**
     * 현재 단계(phase)를 갱신한다. 배치 카운터는 기존 값을 유지한다.
     * IN  : uploadId - 업로드 식별자 (프론트엔드가 생성한 UUID)
     *       phase    - 현재 단계 ("parsing" | "embedding" | "saving")
     * OUT : 없음
     */
    public void setPhase(String uploadId, String phase) {
        if (uploadId == null || uploadId.isBlank()) return;
        store.compute(uploadId, (k, existing) ->
                existing == null ? new Progress(phase, 0, 0)
                                 : new Progress(phase, existing.current(), existing.total()));
    }

    /**
     * 임베딩 배치 진행 상황을 갱신한다.
     * IN  : uploadId - 업로드 식별자
     *       current  - 완료된 배치 번호 (1부터 시작)
     *       total    - 전체 배치 수
     * OUT : 없음
     */
    public void setBatchProgress(String uploadId, int current, int total) {
        if (uploadId == null || uploadId.isBlank()) return;
        store.put(uploadId, new Progress("embedding", current, total));
    }

    /**
     * 현재 진행 상황을 반환한다.
     * IN  : uploadId - 업로드 식별자
     * OUT : Progress - 진행 상황. uploadId가 없으면 null
     */
    public Progress get(String uploadId) {
        return store.get(uploadId);
    }

    /**
     * 업로드가 완료된 후 메모리에서 상태를 제거한다.
     * IN  : uploadId - 제거할 업로드 식별자
     * OUT : 없음
     */
    public void remove(String uploadId) {
        if (uploadId != null) store.remove(uploadId);
    }

    /** 업로드 진행 상황을 담는 record */
    public record Progress(String phase, int current, int total) {}
}
