package com.madeby.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    @Value("${jwt.secret.key}")
    private String jwtSecret;

    public JwtFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // refresh 경로 예외 처리
            String requestPath = request.getURI().getPath();
            if ("/api/user/refresh".equals(requestPath)) {
                log.info("[JwtFilter] /api/user/refresh 요청 - 필터 제외");
                return chain.filter(exchange); // 필터를 거치지 않고 바로 통과
            }
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    Claims claims = validateJwtToken(token, false); // allowExpired = false
                    log.info("[JWT 검증 성공] 사용자 ID: {}", claims.getSubject());

                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", claims.getSubject())
                            .header("X-User-Role", claims.get("auth", String.class))
                            .header("X-User-Enabled", String.valueOf(claims.get("enabled", Boolean.class)))
                            .header("X-User-EmailHash", claims.get("emailHash", String.class))
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } catch (ExpiredJwtException e) {
                    log.warn("[Access Token 만료] 사용자 ID: {}", e.getClaims().getSubject());

                    // 401 Unauthorized 반환
                    return handleUnauthorized(response, "Access token expired. Please refresh the token.");
                } catch (Exception e) {
                    log.error("[JWT 검증 오류] {}", e.getMessage());
                    return handleUnauthorized(response, "Invalid JWT");
                }
            }

            return chain.filter(exchange);
        };
    }


    private Claims validateJwtToken(String token, boolean allowExpired) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            if (allowExpired) {
                log.warn("[JWT 검증] 만료된 토큰 Claims 반환 허용");
                return e.getClaims();
            }
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }


    private Mono<Void> handleUnauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", HttpStatus.UNAUTHORIZED.getReasonPhrase(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Data
    public static class Config {
        private boolean preLogger;
        private boolean postLogger;
    }
}