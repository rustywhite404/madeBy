package com.madeby.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeby.common.ApiResponse;
import com.madeby.dto.LoginRequestDto;
import com.madeby.entity.UserRoleEnum;
import com.madeby.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j(topic = "로그인 및 JWT 생성")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/user/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequestDto requestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);
            log.info("사용자 요청: {}", requestDto.toString());
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
        String email = ((UserDetailsImpl) authResult.getPrincipal()).getUsername();

        UserRoleEnum role = ((UserDetailsImpl) authResult.getPrincipal()).getUser().getRole();
        String token = jwtUtil.createToken(email, role);
        log.info("생성된 JWT: {}", token);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, token);

        // 성공 메시지를 JSON 형태로 작성
        response.setContentType("application/json;charset=UTF-8");
        String successMessage = String.format("{\"message\": \"로그인에 성공하였습니다.\", \"token\": \"%s\"}", token);
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