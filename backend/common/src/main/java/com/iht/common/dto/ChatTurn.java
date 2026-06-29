package com.iht.common.dto;

/**
 * 대화 한 turn을 표현하는 서비스 간 공유 DTO.
 *
 * [왜 필요한가]
 * LLM API는 무상태(stateless)다 - 매 요청마다 지금까지의 대화 전체를 다시 보내야
 * 모델이 "방금 한 질문"의 맥락을 이해한다. 프론트엔드가 이미 화면에 표시할 메시지
 * 목록을 들고 있으므로, 그걸 그대로 이 형태로 변환해서 매 질문마다 함께 보낸다.
 * (백엔드는 DB가 없으므로 대화 이력을 따로 저장하지 않는다 - 완전한 무상태 유지)
 *
 * role    : "user"(사용자) 또는 "assistant"(LLM 답변). Claude/OpenAI 모두 이 두 값을
 *           그대로 쓰기 때문에 변환 없이 바로 전달할 수 있다.
 * content : 해당 turn의 텍스트 내용
 */
public record ChatTurn(
        String role,
        String content
) {}
