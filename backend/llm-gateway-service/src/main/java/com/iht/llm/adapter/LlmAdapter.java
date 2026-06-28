package com.iht.llm.adapter;

import com.iht.common.dto.ChatTurn;

import java.util.List;

/**
 * 외부 LLM Provider 호출을 추상화하는 어댑터 인터페이스.
 *
 * [설계 의도]
 * Claude, OpenAI처럼 호출 방식이 서로 다른 LLM들을 동일한 인터페이스 뒤에 숨긴다.
 * 새로운 Provider(예: Gemini)를 추가하고 싶으면 이 인터페이스의 구현체를 하나 더 만들고
 * @Component만 붙이면 끝난다 - LlmAdapterFactory, LlmController, chat-service 등
 * 다른 코드는 한 줄도 건드릴 필요가 없다. (개방-폐쇄 원칙)
 */
public interface LlmAdapter {

    /**
     * 이 어댑터가 처리하는 provider 식별자를 반환한다. (예: "claude", "openai")
     * LlmAdapterFactory가 요청에 맞는 어댑터를 찾을 때 이 값을 키로 사용한다.
     * OUT : provider 식별자 문자열 (소문자)
     */
    String providerName();

    /**
     * 약관 본문(context), 이전 대화 이력(history), 새 질문(question)을 받아 LLM 응답을 생성한다.
     *
     * IN  : context  - 약관 전체 텍스트. LLM이 답변의 근거로 삼는 자료
     *       question - 사용자가 입력한 이번 turn의 질문
     *       history  - 이전까지의 대화 turn 목록 (role: "user"|"assistant"). LLM은 무상태이므로
     *                  이전 맥락을 이해시키려면 매번 함께 전달해야 한다. 첫 질문이면 빈 리스트가 들어온다.
     * OUT : String - LLM이 생성한 답변 텍스트
     */
    String ask(String context, String question, List<ChatTurn> history);

    /**
     * 사용자 질문을 보험 약관에 나오는 공식 전문 용어로 확장한다.
     * 구현하지 않는 어댑터는 원본 질문을 그대로 반환한다 (기본값).
     *
     * IN  : question - 원본 사용자 질문
     * OUT : String   - 확장된 검색어 (예: "심혈관" → "심혈관, 심근경색, 허혈성심장질환, 협심증")
     */
    default String expandQuery(String question) {
        return question;
    }
}
