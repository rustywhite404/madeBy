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
    private final long TOKEN_TIME = 1 * 60 * 1000L; // 1분

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
            log.info("[JWT 생성] 성공 - 생성된 Access 토큰: ", token);
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
            return refreshToken;
        } catch (Exception e) {
            log.error("[Refresh Token 생성] 오류 - 이메일 해시: {}", emailHash, e);
            throw new RuntimeException("Refresh 토큰 생성 중 오류가 발생했습니다.", e);
        }
    }

    // Refresh Token 검증
    public boolean validateRefreshToken(String refreshToken, String emailHash) {
        log.info("[Refresh Token 검증] 시작 - 이메일 해시: {}", emailHash);
        String storedToken = redisTemplate.opsForValue().get("refreshToken:" + emailHash);
        log.info("--------레디스에 저장된 토큰:"+storedToken);
        log.info("--------비교할 refreshToken:"+refreshToken);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            log.error("[Refresh Token 검증] 실패 - Redis에 저장된 토큰과 요청 토큰이 일치하지 않음");
            return false;
        }

        log.info("[Refresh Token 검증] 성공");
        return true;
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

    // Bearer prefix 제거
    public String removeBearerPrefix(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Authorization 헤더가 null입니다.");
        }
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("[JWT 검증] Expired JWT token, 만료된 JWT token 입니다.");
            return false;
        } catch (Exception e) {
            log.error("[JWT 검증] Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public Claims getUserInfoFromToken(String token, boolean allowExpired) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            if (allowExpired) {
                log.warn("[JWT 정보 추출] 만료된 토큰 허용 - Claims 반환");
                return e.getClaims();
            }
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }


    public String getUserInfoFromRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            return claims.getSubject(); // Refresh Token의 subject는 emailHash로 설정됨
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.", e);
        }
    }

}
