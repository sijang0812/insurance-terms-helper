package com.iht.llm.adapter;

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
     * 약관 본문(context)과 사용자 질문(question)을 받아 LLM 응답을 생성한다.
     * IN  : context  - 약관 전체 텍스트. LLM이 답변의 근거로 삼는 자료
     *       question - 사용자가 입력한 자연어 질문
     * OUT : String - LLM이 생성한 답변 텍스트
     */
    String ask(String context, String question);
}
