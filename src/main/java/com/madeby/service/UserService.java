package com.madeby.service;

import com.madeby.config.EnvironmentConfig;
import com.madeby.dto.SignupRequestDto;
import com.madeby.entity.User;
import com.madeby.entity.UserRoleEnum;
import com.madeby.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvironmentConfig envConfig;

    // ADMIN_TOKEN
    private String ADMIN_TOKEN;

    public void signup(SignupRequestDto requestDto) {
        String userName = requestDto.getUserName();
        String password = passwordEncoder.encode(requestDto.getPassword());

        // 회원 중복 확인
        Optional<User> checkUsername = userRepository.findByUserName(userName);
        if (checkUsername.isPresent()) {
            throw new IllegalArgumentException("중복된 사용자가 존재합니다.");
        }

        // email 중복확인
        String email = requestDto.getEmail();
        Optional<User> checkEmail = userRepository.findByEmail(email);
        if (checkEmail.isPresent()) {
            throw new IllegalArgumentException("중복된 Email 입니다.");
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (requestDto.isAdmin()) {
            ADMIN_TOKEN = envConfig.getAdminToken();
            if (!ADMIN_TOKEN.equals(requestDto.getAdminToken())) {
                throw new IllegalArgumentException("관리자 암호가 틀려 등록이 불가능합니다.");
            }
            role = UserRoleEnum.ADMIN;
        }

        // 사용자 등록
        User user = User.builder()
                .address(requestDto.getAddress())
                .email(requestDto.getEmail())
                .number(requestDto.getNumber())
                .password(password)
                .role(role)
                .userName(userName).build();
        userRepository.save(user);
    }
}
