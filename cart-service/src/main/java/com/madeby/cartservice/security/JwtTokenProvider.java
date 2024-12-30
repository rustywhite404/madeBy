//package com.madeby.cartservice.security;
//
//import com.madeby.cartservice.dto.UserDetailsDto;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import org.springframework.stereotype.Component;
//
//@Component
//public class JwtTokenProvider {
//    String secretKey = "7Iqk7YyM66W07YOA7L2U65Sp7YG065+9U3ByaW5n6rCV7J2Y7Yqc7YSw7LWc7JuQ67mI7J6F64uI64ukLg==";
//    // JWT에서 사용자 정보 추출
//    public UserDetailsDto parseToken(String token) {
//        Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
//        Long userId = claims.get("userId", Long.class);
//        String role = claims.get("role", String.class);
//        boolean isEnabled = claims.get("isEnabled", Boolean.class);
//
//        return new UserDetailsDto(userId, role, isEnabled);
//    }
//}
