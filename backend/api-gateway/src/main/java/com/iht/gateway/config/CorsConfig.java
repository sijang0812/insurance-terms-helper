package com.iht.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Gateway의 CORS 설정.
 * cors.allowed-origins (환경변수 CORS_ALLOWED_ORIGINS)로 허용 오리진을 주입받는다.
 * 쉼표로 여러 개 지정 가능. 기본값은 로컬 개발 서버.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsConfig;

    /**
     * IN  : allowedOriginsConfig - 쉼표 구분 오리진 문자열 (예: "http://localhost:5173,https://yourapp.vercel.app")
     * OUT : 없음
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOriginsConfig.split(",");
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
