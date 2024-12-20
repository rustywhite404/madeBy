package com.madeby.util;

import com.madeby.config.EnvironmentConfig;
import com.madeby.entity.UserRoleEnum;
import io.jsonwebtoken.*;
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

    private RedisTemplate<String, String> redisTemplate;

    // Header KEY 값
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "auth";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";
    // 토큰 만료시간
    private final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    private final EnvironmentConfig environmentConfig;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        // EnvironmentConfig를 사용해 JWT_SECRET_KEY를 로드
        String secretKey = environmentConfig.getJwtSecretKey();
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // 토큰 생성
    public String createToken(String userName, UserRoleEnum role) {
        Date date = new Date();

        return BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(userName) // 사용자 식별자값(ID)
                        .claim(AUTHORIZATION_KEY, role) // 사용자 권한
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME)) // 만료 시간
                        .setIssuedAt(date) // 발급일
                        .signWith(key, signatureAlgorithm) // 암호화 알고리즘
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

    // Access Token 갱신
    public String refreshAccessToken(String refreshToken, String emailHash) {
        // Refresh Token 검증
        if (!validateRefreshToken(refreshToken, emailHash)) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }

        // Refresh Token에서 권한 정보 추출
        Claims claims = getUserInfoFromToken(refreshToken);
        String roleString = claims.get(JwtUtil.AUTHORIZATION_KEY, String.class);
        UserRoleEnum role = UserRoleEnum.valueOf(roleString);

        // 새로운 Access Token 생성
        return createToken(emailHash, role);
    }

    // header 에서 JWT 가져오기
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
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

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}