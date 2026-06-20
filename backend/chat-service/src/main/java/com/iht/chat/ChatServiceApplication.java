package com.iht.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * chat-service의 실행 진입점.
 * 질문-답변 오케스트레이션(document-service + llm-gateway-service 조합)을 전담하는 마이크로서비스.
 */
@SpringBootApplication
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
