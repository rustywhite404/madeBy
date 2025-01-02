package com.madeby.apigateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // CSRF 비활성화
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        // CORS 설정 추가 (필요 시 수정)
        http.cors(cors -> cors.configurationSource(request -> {
            var config = new org.springframework.web.cors.CorsConfiguration();
            config.addAllowedOrigin("*"); // 모든 Origin 허용 (배포 시 수정 필요)
            config.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
            config.addExposedHeader("Authorization");
            config.addAllowedHeader("*"); // 모든 헤더 허용
            return config;
        }));

        // 인증 및 권한 설정
        http.authorizeExchange(exchange -> exchange
                .pathMatchers("/api/user/**").permitAll() // 특정 경로 허용
                .pathMatchers("/api/cart/**").permitAll() // 특정 경로 허용
                .pathMatchers("/api/orders/**").permitAll() // 특정 경로 허용
                .pathMatchers("/api/products", "/api/products/**").permitAll() // 특정 경로 허용
                .anyExchange().authenticated() // 나머지 요청은 인증 필요
        );

        // 기본 로그인, 세션 비활성화
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        return http.build();
    }
}
