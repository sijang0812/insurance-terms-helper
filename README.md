# 약관 쉽게 (Insurance Terms Helper)

보험 약관 PDF를 업로드하면 채팅으로 쉽게 풀어 설명해주는 서비스.

## 구조

- `backend/` - Spring Boot 멀티모듈 (MSA)
  - `common` - 공통 응답 포맷 / 예외 / 서비스 간 공유 DTO (순수 자바, 프레임워크 의존성 없음)
  - `api-gateway` (8080) - 단일 진입점, 라우팅 + CORS
  - `document-service` (8081) - PDF 업로드, 텍스트 추출, 청크 분할, 임베딩 저장, 유사도 검색
    (Neon PostgreSQL + pgvector 사용)
  - `chat-service` (8082) - 질문-답변 오케스트레이션
  - `llm-gateway-service` (8083) - Claude/OpenAI 어댑터 + OpenAI 임베딩
- `frontend/vue-app/` - Vue 3 + Vite

## RAG(검색 증강 생성) 구조

약관이 최대 100만 자에 달할 수 있어, 매 질문마다 전체를 LLM에 보내지 않는다.

```
업로드 시:  PDF → 텍스트 추출 → 1500자 단위 청크 분할 → 각 청크 임베딩(OpenAI) → Neon(pgvector)에 저장
질문 시:    질문 → 임베딩(OpenAI) → pgvector 코사인 유사도 검색(상위 5개 청크) → 그 청크들만 LLM 컨텍스트로 사용
```

임베딩은 답변 생성에 Claude를 쓰든 OpenAI를 쓰든 **항상 OpenAI**(`text-embedding-3-small`)를 사용한다.
즉 Claude만 쓰고 싶어도 OpenAI 키는 필요하다.

## 로컬 실행 방법

### 0. Neon(PostgreSQL + pgvector) 설정

1. https://neon.tech 에서 가입 → 새 프로젝트 생성
2. 프로젝트 대시보드의 **Connection string** 에서 언어 선택을 **Java**로 바꾸면
   `jdbc:postgresql://...` 형태의 URL을 바로 보여준다. 이걸 그대로 사용하면 된다.
3. pgvector 확장은 따로 설치할 필요 없다 - `schema.sql`이 기동 시 자동으로
   `CREATE EXTENSION IF NOT EXISTS vector`를 실행한다.

> **Neon 연결 풀(pooler) 관련 참고**
> Neon은 "pooled"(호스트에 `-pooler` 붙음)와 "direct" 두 가지 연결 문자열을 줍니다.
> 둘 다 동작은 하지만, 만약 나중에 `prepared statement` 관련 에러가 나면 direct
> 연결 문자열로 바꿔보세요. (PgBouncer 트랜잭션 모드와 JDBC의 statement 캐싱이
> 충돌하는 흔한 케이스입니다)

### 1. 백엔드 (서비스 4개를 각각 실행해야 함)

IntelliJ IDEA에서 `backend` 폴더를 Gradle 프로젝트로 열면 4개의 실행 가능한 모듈이 보입니다.
각 모듈의 `*Application.java`를 우클릭 → Run 으로 아래 순서대로 띄워주세요.

1. `DocumentServiceApplication` (8081) - **DB 접속 정보 필요 (아래 참고)**
2. `LlmGatewayServiceApplication` (8083) - **API 키 필요 (아래 참고)**
3. `ChatServiceApplication` (8082)
4. `GatewayApplication` (8080)

> **민감한 값(키/DB 비밀번호) 등록 방법 - `application-local.yml` 추천**
> 두 모듈 각각에 `application-local.yml`을 만들고, IntelliJ 실행 구성의
> `Active profiles`에 `local`을 입력하세요. 이 파일들은 `.gitignore`에 등록되어
> 있어 git에 올라가지 않습니다.
>
> `document-service/src/main/resources/application-local.yml`
> ```yaml
> spring:
>   datasource:
>     url: jdbc:postgresql://ep-xxxx.neon.tech/neondb?sslmode=require
>     username: neondb_owner
>     password: 실제비밀번호
> ```
>
> `llm-gateway-service/src/main/resources/application-local.yml`
> ```yaml
> llm:
>   claude:
>     api-key: sk-ant-실제키
>   openai:
>     api-key: sk-실제키
> ```

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

- **DB 연동 완료 (Neon + pgvector)**: 문서 메타정보와 청크+임베딩이 이제 Postgres에
  영구 저장됩니다. 서버를 재시작해도 데이터가 남아있습니다.
- **LLM 연동 완료 (Claude + OpenAI)**: llm-gateway-service의 ClaudeAdapter / OpenAiAdapter가
  실제 API를 호출합니다. 대화 이력(history)도 프론트엔드가 매 요청마다 함께 보내 멀티턴
  대화를 지원합니다.
- **RAG 적용 완료**: 약관 전체를 매번 보내지 않고, pgvector로 검색된 관련 청크 5개만
  LLM에 전달합니다.
- **빌드/DB 연결 미검증**: 이 코드는 샌드박스 환경의 네트워크 제약으로 Maven Central과
  Neon에 접근할 수 없어 실제 빌드와 DB 연결을 테스트하지 못했습니다. 특히 pgvector
  JDBC 연동(`com.pgvector.PGvector`)은 처음 적용하는 부분이라 에러가 날 가능성이 평소보다
  높습니다. 에러 메시지를 그대로 공유해주세요 - 같이 고치겠습니다. (common 모듈은 외부
  의존성이 없어 별도로 javac 컴파일 검증을 마쳤습니다)
- **프론트엔드는 검증 완료**: `npm install && npm run build`가 실제로 정상 동작하는 것까지
  확인했습니다. (이번 RAG 변경은 백엔드 내부 구조만 바뀐 것이라 프론트엔드 코드는
  수정하지 않았습니다)
