package com.madeby.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration // 아래 설정을 등록하여 활성화
@EnableJpaAuditing // 시간 자동 변경
public class JpaConfig {
}