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
import java.util.Enumeration;

@Slf4j(topic = "JWT 검증 및 인가")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        String userId = req.getHeader("X-User-Id");
        String userRole = req.getHeader("X-User-Role");

        if (userId != null && userRole != null) {
            log.info("[인증 설정] 사용자 ID: {}, 역할: {}", userId, userRole);

            // SecurityContext 설정
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

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
