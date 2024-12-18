package com.madeby.service;

import com.madeby.config.EnvironmentConfig;
import com.madeby.dto.SignupRequestDto;
import com.madeby.entity.User;
import com.madeby.entity.UserRoleEnum;
import com.madeby.exception.MadeByErrorCode;
import com.madeby.exception.MadeByException;
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
        String password = passwordEncoder.encode(requestDto.getPassword());

        // email 중복확인
        String email = requestDto.getEmail();
        Optional<User> checkEmail = userRepository.findByEmail(email);
        if (checkEmail.isPresent()) {
            throw new MadeByException(MadeByErrorCode.DUPLICATED_EMAIL);
        }

        // 핸드폰번호 중복확인
        Optional<User> checkNumber = userRepository.findByNumber(requestDto.getNumber());
        if (checkNumber.isPresent()) {
            throw new MadeByException(MadeByErrorCode.DUPLICATED_NUMBER);
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (requestDto.isAdmin()) {
            ADMIN_TOKEN = envConfig.getAdminToken();
            if (!ADMIN_TOKEN.equals(requestDto.getAdminToken())) {
                throw new MadeByException(MadeByErrorCode.WRONG_ADMIN_TOKEN);
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
                .userName(requestDto.getUserName()).build();
        userRepository.save(user);
    }
}
