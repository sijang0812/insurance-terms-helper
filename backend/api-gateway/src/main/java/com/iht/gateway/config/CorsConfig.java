package com.iht.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Gateway의 CORS 설정.
 * Vue.js 개발 서버(기본 포트 5173, Vite 기준)에서의 요청을 허용한다.
 *
 * [주의] 실제 도메인으로 배포한 뒤에는 allowedOrigins에 운영 도메인
 * (예: "https://yourdomain.com")을 반드시 추가해야 한다. 지금은 로컬 개발용 설정이다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
