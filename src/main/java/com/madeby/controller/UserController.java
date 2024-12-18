package com.madeby.controller;

import com.madeby.common.ApiResponse;
import com.madeby.dto.SignupRequestDto;
import com.madeby.dto.UserInfoDto;
import com.madeby.entity.UserRoleEnum;
import com.madeby.security.UserDetailsImpl;
import com.madeby.service.UserService;
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
    public ResponseEntity<Object> signup(@Valid @RequestBody SignupRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors); // 반환 타입은 Map<String, String>
        }

        userService.signup(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Signup successful. Please login.")); // 반환 타입은 String
    }


    // 회원 관련 정보 받기
    @GetMapping("/user-info")
    @ResponseBody
    public UserInfoDto getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String userName = userDetails.getUser().getUserName();
        UserRoleEnum role = userDetails.getUser().getRole();
        boolean isAdmin = (role == UserRoleEnum.ADMIN);

        return new UserInfoDto(userName, isAdmin);
    }
}