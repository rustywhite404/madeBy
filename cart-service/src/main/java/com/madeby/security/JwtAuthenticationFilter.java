package com.madeby.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeBy.shared.common.ApiResponse;
import com.madeby.dto.LoginRequestDto;
import com.madeby.entity.UserRoleEnum;
import com.madeby.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j(topic = "로그인 및 JWT 생성")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/user/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequestDto requestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getEmail(),
                            requestDto.getPassword(),
                            null
                    )
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
        String emailHash = ((UserDetailsImpl) authResult.getPrincipal()).getUser().getEmailHash(); // 해시값 가져오기
        UserRoleEnum role = ((UserDetailsImpl) authResult.getPrincipal()).getUser().getRole();

        //accessToken 생성
        String accessToken = jwtUtil.createToken(emailHash, role);
        log.info("생성된 Access Token: {}", accessToken);

        // Refresh Token 처리
        String refreshToken = redisTemplate.opsForValue().get("refreshToken:" + emailHash); // 기존 Refresh Token 확인

        if (refreshToken == null) {
            // Refresh Token이 없는 경우에만 새로 생성
            refreshToken = jwtUtil.createRefreshToken(emailHash);
            redisTemplate.opsForValue().set(
                    "refreshToken:" + emailHash,
                    refreshToken,
                    Duration.ofDays(14)
            );
        }
        log.info("생성된 refresh Token: {}", refreshToken);

        // 응답 헤더에 토큰 추가
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, accessToken);
        response.addHeader("Refresh-Token", refreshToken);

        // 성공 메시지를 JSON 형태로 작성
        response.setContentType("application/json;charset=UTF-8");
        String successMessage = String.format("{\"message\": \"로그인에 성공하였습니다.\", \"accessToken\": \"%s\", \"refreshToken\": \"%s\"}", accessToken, refreshToken);
        response.getWriter().write(successMessage);
        response.getWriter().flush();
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        // 상태 코드 초기값
        int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
        String errorCode = "AUTHENTICATION_FAILED";
        String errorMessage = "잘못된 로그인 정보입니다.";

        // 쿠키 삭제
        Cookie cookie = new Cookie("Authorization", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        errorMessage = failed.getMessage();

        // 응답 객체 생성
        ApiResponse<?> errorResponse = ApiResponse.failure(errorCode, errorMessage);

        // JSON 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        // 응답 설정
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

}