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
        log.info("[JWT 초기화] - 비밀키 설정 시작");
        String secretKey = "7Iqk7YyM66W07YOA7L2U65Sp7YG065+9U3ByaW5n6rCV7J2Y7Yqc7YSw7LWc7JuQ67mI7J6F64uI64ukLg==";
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
        log.info("[JWT 초기화] - 비밀키 설정 완료");
    }

    // Access Token 생성
    public String createToken(Long userId, String emailHash, UserRoleEnum role, boolean isEnabled) {
        log.info("[JWT 생성] 시작 - 사용자 ID: {}, 이메일 해시: {}, 권한: {}, 활성화 여부: {}", userId, emailHash, role, isEnabled);
        Date now = new Date();
        log.info("[JWT 생성] 현재 시간: {}", now);
        Date expirationDate = new Date(now.getTime() + TOKEN_TIME);
        log.info("[JWT 생성] 만료 시간: {}", expirationDate);

        try {
            String token = Jwts.builder()
                    .setSubject(String.valueOf(userId))
                    .claim("emailHash", emailHash)
                    .claim(AUTHORIZATION_KEY, role)

                    .claim("enabled", isEnabled)
                    .setExpiration(expirationDate)
                    .setIssuedAt(now)
                    .signWith(key, signatureAlgorithm)
                    .compact();
            log.info("[JWT 생성] 성공 - 생성된 토큰: Bearer {}", token);
            return BEARER_PREFIX + token;
        } catch (Exception e) {
            log.error("[JWT 생성] 오류 - 사용자 ID: {}, 이메일 해시: {}, 권한: {}, 활성화 여부: {}", userId, emailHash, role, isEnabled, e);
            throw new RuntimeException("JWT 토큰 생성 중 오류가 발생했습니다.", e);
        }
    }

    // 여기
    public String createRefreshToken(String emailHash) {
        log.info("[Refresh Token 생성] 시작 - 이메일 해시: {}", emailHash);
        Date now = new Date();
        log.info("[Refresh Token 생성] 현재 시간: {}", now);
        long refreshTokenValidity = 14 * 24 * 60 * 60 * 1000L; // 2주
        Date expirationDate = new Date(now.getTime() + refreshTokenValidity);
        log.info("[Refresh Token 생성] 만료 시간: {}", expirationDate);

        try {
            String refreshToken = Jwts.builder()
                    .setSubject(emailHash)
                    .setExpiration(expirationDate)
                    .setIssuedAt(now)
                    .signWith(key, signatureAlgorithm)
                    .compact();
            log.info("[Refresh Token 생성] 성공 - 생성된 Refresh 토큰: {}", refreshToken);
            return BEARER_PREFIX + refreshToken;
        } catch (Exception e) {
            log.error("[Refresh Token 생성] 오류 - 이메일 해시: {}", emailHash, e);
            throw new RuntimeException("Refresh 토큰 생성 중 오류가 발생했습니다.", e);
        }
    }

    public boolean validateRefreshToken(String refreshToken, String emailHash) {
        log.info("[Refresh Token 검증] 시작 - 이메일 해시: {}", emailHash);
        String storedToken = redisTemplate.opsForValue().get("refreshToken:" + emailHash);

        if (storedToken == null) {
            log.error("[Refresh Token 검증] Redis에 저장된 토큰이 없습니다. 키: {}", "refreshToken:" + emailHash);
            return false;
        }

        log.info("[Refresh Token 검증] Redis에서 가져온 토큰: {}", storedToken);
        log.info("[Refresh Token 검증] 요청에서 전달된 토큰: {}", refreshToken);

        boolean isValid = storedToken.equals(refreshToken);
        if (!isValid) {
            log.error("[Refresh Token 검증] 실패 - Redis 토큰과 요청 토큰이 불일치합니다.");
        }
        return isValid;
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        log.info("[JWT 헤더 추출] 요청에서 Authorization 헤더 확인");
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        log.info("[JWT 헤더 추출] Authorization 헤더 값: {}", bearerToken);

        String token = removeBearerPrefix(bearerToken);
        if (StringUtils.hasText(token)) {
            log.info("[JWT 헤더 추출] 추출된 토큰 값: {}", token);
            return token;
        }

        log.warn("[JWT 헤더 추출] Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        return null;
    }

    private String removeBearerPrefix(String token) {
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
    }

    public Long extractUserId(String token) {
        log.info("[사용자 ID 추출] 토큰에서 사용자 ID 추출 시작");
        Claims claims = getUserInfoFromToken(token);
        Long userId = Long.valueOf(claims.getSubject());
        log.info("[사용자 ID 추출] 추출된 사용자 ID: {}", userId);
        return userId;
    }

    public boolean validateToken(String token) {
        log.info("[JWT 검증] 토큰 검증 시작");
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            log.info("[JWT 검증] 토큰 유효성 검증 성공");
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("[JWT 검증] Invalid JWT signature, 유효하지 않은 JWT 서명 입니다.", e);
        } catch (ExpiredJwtException e) {
            log.error("[JWT 검증] Expired JWT token, 만료된 JWT token 입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.error("[JWT 검증] Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.", e);
        } catch (IllegalArgumentException e) {
            log.error("[JWT 검증] JWT claims is empty, 잘못된 JWT 토큰 입니다.", e);
        }
        log.warn("[JWT 검증] 토큰이 유효하지 않습니다.");
        return false;
    }

    public Claims getUserInfoFromToken(String token) {
        log.info("[JWT 정보 추출] 토큰에서 정보 추출 시작");
        token = removeBearerPrefix(token);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.info("[JWT 정보 추출] 추출된 Claims: {}", claims);
            return claims;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT 정보 추출] JWT 토큰이 만료되었습니다.", e);
            return e.getClaims();
        } catch (Exception e) {
            log.error("[JWT 정보 추출] JWT 파싱 중 오류 발생.", e);
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }



}
