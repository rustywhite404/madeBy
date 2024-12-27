package com.madeby.orderservice.security;

import com.madeby.orderservice.entity.UserRoleEnum;
import com.madeby.orderservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public JwtAuthorizationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        String tokenValue = jwtUtil.getJwtFromHeader(req);

        if (StringUtils.hasText(tokenValue)) {
            try {
                // Access Token 검증
                log.info("Access Token 검증...");
                Claims info = jwtUtil.getUserInfoFromToken(tokenValue);
                setAuthentication(info.getSubject());
            } catch (ExpiredJwtException e) {
                log.info("Access Token이 만료되었습니다. Refresh Token 확인 중...");
                String emailHash = e.getClaims().getSubject(); // 만료된 토큰에서 subject 추출

                // Redis에서 Refresh Token 조회
                String refreshToken = redisTemplate.opsForValue().get("refreshToken:" + emailHash);
                if (refreshToken != null && jwtUtil.validateToken(refreshToken)) {
                    log.info("유효한 Refresh Token 존재. 새로운 Access Token 발급 중...");
                    UserRoleEnum role = jwtUtil.getUserInfoFromToken(refreshToken).get("auth", UserRoleEnum.class);
                    String newAccessToken = jwtUtil.createToken(emailHash, role);

                    // 응답 헤더에 새로운 Access Token 추가
                    res.addHeader(JwtUtil.AUTHORIZATION_HEADER, newAccessToken);
                    log.info("새로운 Access Token 발급 완료: {}", newAccessToken);

                    // Security Context에 인증 정보 설정
                    setAuthentication(emailHash);
                } else {
                    log.error("유효한 Refresh Token이 없음. 인증 실패.");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\": \"Refresh Token이 만료되었습니다. 다시 로그인해주세요.\"}");
                    return;
                }
            } catch (Exception ex) {
                log.error("JWT 처리 중 오류 발생: {}", ex.getMessage());
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write("{\"error\": \"잘못된 토큰입니다.\"}");
                return;
            }
        }

        filterChain.doFilter(req, res);
    }

    // 인증 처리
    public void setAuthentication(String userName) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(userName);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    // 인증 객체 생성
    private Authentication createAuthentication(String userName) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}