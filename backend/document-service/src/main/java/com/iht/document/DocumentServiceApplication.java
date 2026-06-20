package com.iht.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * document-service의 실행 진입점.
 * PDF 업로드와 텍스트 추출만을 전담하는 마이크로서비스.
 */
@SpringBootApplication
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
