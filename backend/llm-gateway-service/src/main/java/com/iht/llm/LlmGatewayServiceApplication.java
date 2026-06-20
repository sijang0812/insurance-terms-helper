package com.iht.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * llm-gateway-service의 실행 진입점.
 * Claude, OpenAI 등 외부 LLM 호출만을 전담하는 마이크로서비스.
 */
@SpringBootApplication
public class LlmGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmGatewayServiceApplication.class, args);
    }
}
