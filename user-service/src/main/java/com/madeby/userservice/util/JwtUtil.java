package com.madeby.userservice.util;

import com.madeBy.shared.entity.UserRoleEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final RedisTemplate<String, String> redisTemplate;

    // Header KEY 값
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "auth";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";

    // 토큰 만료시간
    private final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        // EnvironmentConfig를 사용해 JWT_SECRET_KEY를 로드
        String secretKey = "7Iqk7YyM66W07YOA7L2U65Sp7YG065+9U3ByaW5n6rCV7J2Y7Yqc7YSw7LWc7JuQ67mI7J6F64uI64ukLg==";
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // Access Token 생성
    public String createToken(Long userId, String emailHash, UserRoleEnum role, boolean isEnabled) {
        Date date = new Date();

        return BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(String.valueOf(userId)) // userId를 subject로 설정
                        .claim("emailHash", emailHash) // emailHash를 클레임으로 추가
                        .claim(AUTHORIZATION_KEY, role) // 사용자 권한 추가
                        .claim("enabled", isEnabled) // 활성화 상태 추가
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME)) // 만료 시간
                        .setIssuedAt(date) // 발급일
                        .signWith(key, signatureAlgorithm) // 서명
                        .compact();
    }

    // refresh Token 생성
    public String createRefreshToken(String emailHash) {
        Date now = new Date();
        long refreshTokenValidity = 14 * 24 * 60 * 60 * 1000L; // 2주

        return Jwts.builder()
                .setSubject(emailHash)
                .setExpiration(new Date(now.getTime() + refreshTokenValidity))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    // Refresh Token 검증
    public boolean validateRefreshToken(String refreshToken, String emailHash) {
        // Redis에서 저장된 Refresh Token 가져오기
        String storedToken = redisTemplate.opsForValue().get("refreshToken:" + emailHash);

        // 저장된 Refresh Token과 비교
        return storedToken != null && storedToken.equals(refreshToken);
    }

    // header 에서 JWT 가져오기
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 사용자 ID 추출
    public Long extractUserId(String token) {
        Claims claims = getUserInfoFromToken(token);
        return Long.valueOf(claims.getSubject()); // Subject를 userId로 간주
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
    }

    //만료된 AccessToken에서 정보 추출
    public Claims getUserInfoFromToken(String token) {
        try {
            // "Bearer " 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7); // "Bearer " 이후의 순수 토큰 값 추출
            }

            // JWT 파싱 및 검증
            return Jwts.parserBuilder()
                    .setSigningKey(key) // 서명 키 설정
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰이 만료되었습니다. 만료된 토큰으로 요청을 처리하려고 시도했습니다.");
            throw new IllegalArgumentException("만료된 토큰입니다.", e);
        } catch (MalformedJwtException e) {
            log.error("JWT 구조가 올바르지 않습니다: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 토큰 형식입니다.", e);
        } catch (DecodingException e) {
            log.error("JWT 디코딩 중 문제가 발생했습니다: {}", e.getMessage());
            throw new IllegalArgumentException("JWT 디코딩 중 문제가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("JWT 파싱 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }


}