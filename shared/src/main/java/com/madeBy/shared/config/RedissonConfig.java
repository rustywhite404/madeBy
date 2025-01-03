package com.madeBy.shared.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379") // Redis 서버 주소
                .setConnectionMinimumIdleSize(10)    // 최소 연결 유지 수
                .setConnectionPoolSize(64)          // 최대 연결 수
                .setIdleConnectionTimeout(10000);   // 유휴 연결 타임아웃

        // JacksonCodec을 사용해 JSON 직렬화/역직렬화
        config.setCodec(new JsonJacksonCodec());

        return Redisson.create(config);
    }
}
