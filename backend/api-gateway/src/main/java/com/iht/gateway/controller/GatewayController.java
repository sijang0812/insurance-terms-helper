package com.iht.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 단일 진입점 API Gateway 컨트롤러.
 *
 * [설계 의도]
 * 프론트엔드는 이 서비스(8080 포트) 하나만 알면 되고, 내부적으로 어떤 마이크로서비스가
 * 실제 요청을 처리하는지는 신경 쓰지 않아도 된다. 추후 서비스가 늘어나거나 내부 URL이
 * 바뀌어도 프론트엔드 코드는 전혀 수정할 필요가 없다.
 *
 * 지금 단계에서는 Spring Cloud Gateway 같은 별도 프레임워크 없이,
 * RestClient로 직접 요청을 전달(proxy)하는 가벼운 방식으로 구현했다.
 * 이유: (1) 서비스 디스커버리가 아직 필요 없는 작은 규모이고, (2) 의존성 버전 호환성
 * 이슈를 피하기 위함이다. 라우팅 규칙이 복잡해지거나 트래픽이 늘어나면
 * Spring Cloud Gateway 도입을 다시 검토한다.
 */
@RestController
public class GatewayController {

    private final RestClient documentServiceClient;
    private final RestClient chatServiceClient;

    /**
     * IN : documentServiceUrl - application.yml의 services.document-service.url
     *      chatServiceUrl     - application.yml의 services.chat-service.url
     */
    public GatewayController(
            @Value("${services.document-service.url}") String documentServiceUrl,
            @Value("${services.chat-service.url}") String chatServiceUrl) {
        this.documentServiceClient = RestClient.builder().baseUrl(documentServiceUrl).build();
        this.chatServiceClient = RestClient.builder().baseUrl(chatServiceUrl).build();
    }

    /**
     * PDF 업로드 요청을 document-service로 그대로 전달한다.
     * IN  : file     - 사용자가 업로드한 PDF 파일 (multipart/form-data, "file" 파트)
     *       uploadId - 진행률 추적용 식별자 (선택 사항, 없으면 빈 문자열)
     * OUT : document-service가 응답한 JSON 본문을 그대로 전달 (상태 코드 포함)
     */
    @PostMapping(value = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadId", required = false, defaultValue = "") String uploadId) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        Resource fileResource = file.getResource();
        parts.add("file", fileResource);

        String uri = uploadId.isBlank() ? "/api/documents" : "/api/documents?uploadId=" + uploadId;
        return documentServiceClient.post()
                .uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .exchange(this::passThrough);
    }

    /**
     * 업로드 진행 상황 폴링 요청을 document-service로 그대로 전달한다.
     * IN  : body - {"uploadId": "..."} JSON 요청 본문
     * OUT : document-service가 응답한 JSON 본문을 그대로 전달
     */
    @PostMapping(value = "/api/documents/upload-progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getUploadProgress(@RequestBody String body) {
        return documentServiceClient.post()
                .uri("/api/documents/upload-progress")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange(this::passThrough);
    }

    /**
     * 채팅 질문 요청을 chat-service로 그대로 전달한다.
     * IN  : body - {documentId, question, provider} 형태의 JSON 요청 본문 (가공하지 않고 그대로 전달)
     * OUT : chat-service가 응답한 JSON 본문을 그대로 전달
     */
    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@RequestBody String body) {
        return chatServiceClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange(this::passThrough);
    }

    /**
     * 다운스트림 서비스(document-service, chat-service)의 응답을 가공 없이 그대로 옮겨 담는다.
     *
     * [왜 필요한가]
     * RestClient의 retrieve()는 응답이 4xx/5xx면 기본적으로 예외를 던져버려서,
     * 게이트웨이가 다운스트림의 실제 상태 코드(예: 404, 400)를 프론트엔드에 그대로
     * 전달하지 못하는 문제가 있다. exchange()는 상태 코드와 무관하게 원문 응답을
     * 그대로 다룰 수 있게 해주므로, 게이트웨이 본연의 역할인 "투명하게 전달하기"에 맞다.
     *
     * [헤더는 전부 복사하지 않고 Content-Type만 옮긴다]
     * 다운스트림 응답의 Content-Length, Transfer-Encoding 같은 헤더를 그대로 복사하면,
     * 본문을 문자열로 한 번 더 읽고 쓰는 과정에서 실제 바이트 수와 헤더 값이 어긋나
     * 클라이언트가 응답을 깨진 것으로 인식할 수 있다. 지금은 JSON 응답만 다루므로
     * Content-Type 정도만 옮겨도 충분하다.
     *
     * IN  : request  - 게이트웨이가 보낸 원본 요청 (사용하지 않지만 시그니처상 필요)
     *       response - 다운스트림 서비스로부터 받은 원본 응답
     * OUT : ResponseEntity<String> - 다운스트림 응답의 상태코드/본문을 그대로 옮긴 응답
     */
    private ResponseEntity<String> passThrough(
            org.springframework.http.HttpRequest request,
            org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        MediaType contentType = response.getHeaders().getContentType();

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
        if (contentType != null) {
            builder.contentType(contentType);
        }
        return builder.body(body);
    }
}
