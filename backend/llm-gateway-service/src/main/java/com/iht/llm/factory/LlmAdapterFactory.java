package com.iht.llm.factory;

import com.iht.common.exception.BusinessException;
import com.iht.common.exception.ErrorCode;
import com.iht.llm.adapter.LlmAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * provider 이름("claude", "openai" 등)에 맞는 LlmAdapter 구현체를 찾아주는 팩토리.
 *
 * [동작 원리]
 * 생성자에서 Spring이 LlmAdapter 타입의 빈을 전부 List로 주입해준다.
 * (현재는 ClaudeAdapter, OpenAiAdapter 2개가 자동으로 주입된다)
 * 새로운 어댑터가 추가되어도 이 클래스는 코드를 한 줄도 고칠 필요가 없다.
 */
@Component
public class LlmAdapterFactory {

    private final Map<String, LlmAdapter> adapterMap;

    /**
     * IN : adapters - Spring 컨테이너에 등록된 모든 LlmAdapter 구현체 목록 (자동 주입)
     */
    public LlmAdapterFactory(List<LlmAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(LlmAdapter::providerName, Function.identity()));
    }

    /**
     * provider 이름으로 알맞은 LlmAdapter를 반환한다.
     * IN  : provider - "claude", "openai" 등 클라이언트가 지정한 provider 이름
     * OUT : LlmAdapter - 해당 provider를 처리하는 어댑터 구현체
     * @throws BusinessException 등록되지 않은 provider 이름이 들어온 경우 (LLM_PROVIDER_NOT_SUPPORTED)
     */
    public LlmAdapter getAdapter(String provider) {
        LlmAdapter adapter = adapterMap.get(provider);
        if (adapter == null) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_SUPPORTED);
        }
        return adapter;
    }
}
