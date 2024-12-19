package com.madeby.controller;

import com.madeby.common.ApiResponse;
import com.madeby.dto.SignupRequestDto;
import com.madeby.dto.UserInfoDto;
import com.madeby.entity.User;
import com.madeby.entity.UserRoleEnum;
import com.madeby.security.UserDetailsImpl;
import com.madeby.service.UserService;
import com.madeby.util.AES256Util;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @PostMapping("/user/signup")
    public ResponseEntity<Object> signup(@Valid @RequestBody SignupRequestDto requestDto, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors); // 반환 타입은 Map<String, String>
        }

        userService.signup(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Signup successful. Please login.")); // 반환 타입은 String
    }


    // 회원 정보 조회(이름, 관리자 여부만 리턴)
    @GetMapping("/user-info")
    @ResponseBody
    public UserInfoDto getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails){

        try {
            String encryptedUserName = userDetails.getUser().getUserName();
            String userName = AES256Util.decryptWithIV(encryptedUserName);
            UserRoleEnum role = userDetails.getUser().getRole();
            boolean isAdmin = (role == UserRoleEnum.ADMIN);
            return new UserInfoDto(userName, isAdmin);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting user data", e);
        }

    }

    // 회원 정보 조회(전체 정보 리턴)
    @GetMapping("/user-infoAll")
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

}