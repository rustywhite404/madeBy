package com.madeby.userservice.security;

import com.madeBy.shared.entity.UserRoleEnum;
import com.madeby.userservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

@Slf4j(topic = "JWT 검증 및 인가")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        log.info("[필터 실행] 요청 URI: {}", req.getRequestURI());
        String tokenValue = jwtUtil.getJwtFromHeader(req);
        log.info("[토큰 검증] Authorization 헤더에서 추출된 토큰: {}", tokenValue);

        String requestURI = req.getRequestURI(); // 요청 URI 확인

        if (StringUtils.hasText(tokenValue)) {
            try {
                // /api/user/refresh 요청에서는 만료된 Claims 허용
                boolean allowExpired = "/api/user/refresh".equals(requestURI);
                Claims info = jwtUtil.getUserInfoFromToken(tokenValue, allowExpired);

                if (!allowExpired) {
                    log.info("[Access Token 검증] 성공 - 사용자 ID: {}", info.get("userId", Long.class));
                    setAuthentication(info.get("userId", Long.class));
                } else {
                    log.info("[만료된 Claims 처리] /api/user/refresh 요청 - Claims 저장");
                    req.setAttribute("expiredClaims", info); // 만료된 Claims를 요청 속성에 추가
                }

            } catch (ExpiredJwtException e) {
                // 만료된 토큰 처리
                log.warn("[Access Token 만료] 만료된 토큰 - 사용자: {}", e.getClaims().getSubject());
                if ("/api/user/refresh".equals(requestURI)) {
                    log.info("[만료된 Claims 허용] /api/user/refresh 요청 - Claims 저장");
                    req.setAttribute("expiredClaims", e.getClaims()); // 만료된 Claims를 요청 속성에 추가
                } else {
                    // 만료된 토큰을 사용하는 다른 요청은 차단
                    log.error("[Access Token 오류] 만료된 토큰 - 사용자 ID: {}", e.getClaims().getSubject());
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\": \"Token expired\"}");
                    return;
                }
            } catch (Exception ex) {
                // 기타 토큰 처리 오류
                log.error("[토큰 처리 오류] JWT 처리 중 오류 발생: {}", ex.getMessage());
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\": \"잘못된 토큰입니다.\"}");
                return;
            }
        } else {
            log.info("[토큰 없음] Authorization 헤더에 유효한 토큰이 없습니다.");
        }

        log.info("[필터 체인] 다음 필터로 진행");
        filterChain.doFilter(req, res);
    }



    private void setAuthentication(Long userId) {
        log.info("[인증 설정] 사용자 ID로 SecurityContextHolder 설정 시작 - ID: {}", userId);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("[인증 설정] SecurityContextHolder 설정 완료");
    }

    private Authentication createAuthentication(String userName) {
        log.info("[인증 객체 생성] 사용자 이름으로 인증 객체 생성 시작 - 사용자 이름: {}", userName);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
        log.info("[인증 객체 생성] UserDetails 로드 완료: {}", userDetails);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        log.info("[인증 객체 생성] 인증 객체 생성 완료");
        return authentication;
    }
}
