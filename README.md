# 약관 쉽게 (Insurance Terms Helper)

보험 약관 PDF를 업로드하면 채팅으로 쉽게 풀어 설명해주는 서비스.

## 구조

- `backend/` - Spring Boot 멀티모듈 (MSA)
  - `common` - 공통 응답 포맷 / 예외 / 서비스 간 공유 DTO (순수 자바, 프레임워크 의존성 없음)
  - `api-gateway` (8080) - 단일 진입점, 라우팅 + CORS
  - `document-service` (8081) - PDF 업로드 및 텍스트 추출
  - `chat-service` (8082) - 질문-답변 오케스트레이션
  - `llm-gateway-service` (8083) - Claude/OpenAI 어댑터 (현재 스텁 상태)
- `frontend/vue-app/` - Vue 3 + Vite

## 로컬 실행 방법

### 1. 백엔드 (서비스 4개를 각각 실행해야 함)

IntelliJ IDEA에서 `backend` 폴더를 Gradle 프로젝트로 열면 4개의 실행 가능한 모듈이 보입니다.
각 모듈의 `*Application.java`를 우클릭 → Run 으로 아래 순서대로 띄워주세요. (순서는 크게
상관없지만, document-service / llm-gateway-service를 먼저 띄우는 게 디버깅하기 편합니다)

1. `DocumentServiceApplication` (8081)
2. `LlmGatewayServiceApplication` (8083)
3. `ChatServiceApplication` (8082)
4. `GatewayApplication` (8080)

> **Gradle Wrapper 관련 안내**
> 이 프로젝트는 `gradlew` 파일을 포함하고 있지 않습니다. IntelliJ에서 `backend` 폴더를 열면
> "Gradle 프로젝트를 가져올까요?" 라는 안내가 뜨는데, 이때 `Use Gradle from:` 옵션을
> **`IntelliJ IDEA (bundled)`** 로 선택하면 별도 Gradle 설치 없이 바로 빌드할 수 있습니다.

### 2. 프론트엔드

```bash
cd frontend/vue-app
npm install
npm run dev
```

브라우저에서 `http://localhost:5173` 접속.

## 현재 단계에서 알아둘 점

- **DB 미연동**: 업로드된 문서는 document-service 메모리에만 저장됩니다. 서버를 재시작하면
  사라집니다.
- **LLM 미연동**: llm-gateway-service의 ClaudeAdapter / OpenAiAdapter는 아직 실제 API를
  호출하지 않고 더미 답변을 반환합니다. (다음 단계에서 연동 예정)
- **빌드 미검증**: 이 코드는 샌드박스 환경의 네트워크 제약으로 Maven Central에 접근할 수
  없어 실제 `./gradlew build`로 컴파일 검증을 하지 못했습니다. 로컬에서 빌드 시 에러가
  나면 에러 메시지를 그대로 공유해주세요 - 같이 고치겠습니다. (common 모듈은 외부 의존성이
  없어 별도로 javac 컴파일 검증을 마쳤습니다)
- **프론트엔드는 검증 완료**: `npm install && npm run build`가 실제로 정상 동작하는 것까지
  확인했습니다.
