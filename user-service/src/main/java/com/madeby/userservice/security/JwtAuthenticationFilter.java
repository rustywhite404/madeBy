package com.madeby.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeBy.shared.common.ApiResponse;
import com.madeBy.shared.entity.UserRoleEnum;
import com.madeby.userservice.dto.LoginRequestDto;
import com.madeby.userservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j(topic = "로그인 및 JWT 생성")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("[로그인 시도] 사용자 인증 시작");
        try {
            LoginRequestDto requestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);
            log.info("[로그인 시도] 요청 데이터 - 이메일: {}, 비밀번호: [HIDDEN]", requestDto.getEmail());

            Authentication authentication = getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getEmail(),
                            requestDto.getPassword(),
                            null
                    )
            );
            log.info("[로그인 시도] 인증 성공");
            return authentication;
        } catch (IOException e) {
            log.error("[로그인 시도] 요청 데이터 읽기 실패: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();
        Long userId = userDetails.getUser().getId();
        String emailHash = userDetails.getUser().getEmailHash();
        UserRoleEnum role = userDetails.getUser().getRole();
        boolean isEnabled = userDetails.isEnabled();

        String accessToken = jwtUtil.createToken(userId, emailHash, role, isEnabled);
        log.info("[로그인 성공] 완성된 Access Token: {}", accessToken);

        String refreshToken = redisTemplate.opsForValue().get("refreshToken:" + emailHash);
        if (refreshToken == null) {
            refreshToken = jwtUtil.createRefreshToken(emailHash);
            redisTemplate.opsForValue().set(
                    "refreshToken:" + emailHash,
                    refreshToken,
                    Duration.ofDays(14)
            );
            log.info("[로그인 성공] Redis에 새 Refresh Token 저장 완료");
        }

        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, accessToken);
        response.addHeader("Refresh-Token", refreshToken);

        response.setContentType("application/json;charset=UTF-8");
        String successMessage = String.format("{\"message\": \"로그인에 성공하였습니다.\", \"accessToken\": \"%s\", \"refreshToken\": \"%s\"}", accessToken, refreshToken);
        response.getWriter().write(successMessage);
        response.getWriter().flush();
    }


    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        log.info("[로그인 실패] 응답 작성 시작");

        int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
        String errorCode = "AUTHENTICATION_FAILED";
        String errorMessage = "잘못된 로그인 정보입니다.";

        Cookie cookie = new Cookie("Authorization", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        log.info("[로그인 실패] 쿠키 제거 완료");

        errorMessage = failed.getMessage();

        ApiResponse<?> errorResponse = ApiResponse.failure(errorCode, errorMessage);
        log.info("[로그인 실패] 에러 응답 데이터: {}", errorResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.info("[로그인 실패] 응답 작성 완료");
    }

    public void configureLoginUrl() {
        log.info("[설정] 로그인 처리 URL 설정: /api/user/login");
        setFilterProcessesUrl("/api/user/login");
    }
}
