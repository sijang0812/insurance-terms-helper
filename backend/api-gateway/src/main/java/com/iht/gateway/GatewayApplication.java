package com.iht.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * api-gateway의 실행 진입점.
 * 프론트엔드가 호출하는 단일 진입점이며, 실제 비즈니스 로직은 없고 라우팅/CORS만 담당한다.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
