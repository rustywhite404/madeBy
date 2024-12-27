package com.madeby.orderservice.controller;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.common.ApiResponse;
import com.madeby.orderservice.dto.SignupRequestDto;
import com.madeby.orderservice.dto.UserInfoDto;
import com.madeby.orderservice.entity.User;
import com.madeby.orderservice.entity.UserRoleEnum;
import com.madeby.orderservice.repository.UserRepository;
import com.madeby.orderservice.security.UserDetailsImpl;
import com.madeby.orderservice.service.UserService;
import com.madeby.orderservice.util.AES256Util;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;


    @PutMapping("/user/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> requestBody
    ) {
        // 요청에서 현재 비밀번호와 새 비밀번호 추출
        String currentPassword = requestBody.get("currentPassword");
        String newPassword = requestBody.get("newPassword");

        // 비밀번호 변경 서비스 호출
        userService.changePassword(userDetails.getUser(), currentPassword, newPassword);

        // 성공 응답 반환
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 변경 되었습니다."));
    }

    @PostMapping("/user/signup")
    public ResponseEntity<Object> signup(@Valid @RequestBody SignupRequestDto requestDto, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors); // 반환 타입은 Map<String, String>
        }

        userService.signup(requestDto);
        return ResponseEntity.ok(ApiResponse.success("가입이 완료되었습니다. 이메일 인증 후 로그인하세요.")); // 반환 타입은 String
    }


    // 회원 정보 조회(이름, 관리자 여부만 리턴)
    @GetMapping("/user/info")
    @ResponseBody
    public UserInfoDto getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 암호화된 사용자명을 복호화
            String encryptedUserName = userDetails.getUser().getUserName();
            String userName = AES256Util.decryptWithIV(encryptedUserName);

            // 권한 확인
            UserRoleEnum role = userDetails.getUser().getRole();
            boolean isAdmin = (role == UserRoleEnum.ADMIN);

            // 정적 팩토리 메서드로 최소 정보 DTO 생성
            return UserInfoDto.minimalInfo(userName, isAdmin);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting user data", e);
        }
    }


    // 회원 정보 조회(전체 정보 리턴)
    @GetMapping("/user/infoAll")
    @ResponseBody
    public UserInfoDto getUserInfoAll(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();

        return UserInfoDto.builder()
                .userName(user.getUserName())
                .email(user.getEmail())
                .number(user.getNumber())
                .address(user.getAddress())
                .isAdmin(user.getRole() == UserRoleEnum.ADMIN)
                .build();
    }

    @PostMapping("/user/auth/{userId}/{code}")
    public ResponseEntity<Object> verifyEmail(@PathVariable Long userId, @PathVariable String code) {
        // Redis에서 인증 코드 조회
        String redisKey = "email:verification:" + userId;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null || !storedCode.equals(code)) {

            ApiResponse<?> errorResponse = ApiResponse.failure("INVALID_CODE", "유효하지 않거나 만료된 인증 코드입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        // User 엔티티 업데이트
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.USER_NOT_FOUND));
        user.setEmailVerified(true);
        userRepository.save(user);

        // Redis에서 인증 코드 삭제
        redisTemplate.delete(redisKey);

        return ResponseEntity.ok(ApiResponse.success("인증이 완료되었습니다. 로그인 해주세요."));
    }

    @DeleteMapping("/user/logout")
    public ResponseEntity<Object> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            ApiResponse<?> errorResponse = ApiResponse.failure("USER_NOT_AUTH_INFO", MadeByErrorCode.USER_NOT_AUTH_INFO.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }

        // UserDetailsImpl에서 emailHash 추출
        String emailHash = userDetails.getUser().getEmailHash();

        // Refresh Token 삭제
        String redisKey = "refreshToken:" + emailHash;
        if (redisTemplate.hasKey(redisKey)) {
            redisTemplate.delete(redisKey);
        }

        return ResponseEntity.ok().body(ApiResponse.success("로그아웃이 완료 되었습니다."));
    }

}