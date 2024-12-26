package com.madeBy.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Java 8 날짜/시간 타입(LocalDateTime 등)을 Jackson에서
     * 직렬화/역직렬화할 때 ISO-8601 형식으로 처리하기 위해 설정.
     * 기본적으로 Jackson은 Java 8 날짜/시간 타입을 지원하지 않아,
     * 이를 처리하지 않으면 JSON 변환 시 예외가 발생하거나
     * Timestamp 형식으로 잘못 변환될 수 있음.
     */
    private final ObjectMapper objectMapper;

    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        objectMapper.registerModule(new JavaTimeModule()); // JavaTimeModule 등록
    }
}
