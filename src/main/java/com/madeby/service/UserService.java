package com.madeby.service;

import com.madeby.config.EnvironmentConfig;
import com.madeby.dto.SignupRequestDto;
import com.madeby.dto.UserInfoDto;
import com.madeby.entity.User;
import com.madeby.entity.UserRoleEnum;
import com.madeby.exception.MadeByErrorCode;
import com.madeby.exception.MadeByException;
import com.madeby.repository.UserRepository;
import com.madeby.util.AES256Util;
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

    public void signup(SignupRequestDto requestDto) throws Exception {
        String password = passwordEncoder.encode(requestDto.getPassword());

        // email 중복확인
        String encryptedEmail = AES256Util.encryptWithIV(requestDto.getEmail());
        Optional<User> checkEmail = userRepository.findByEmail(encryptedEmail);
        if (checkEmail.isPresent()) {
            throw new MadeByException(MadeByErrorCode.DUPLICATED_EMAIL);
        }

        // 핸드폰번호 중복확인
        String encryptedNumber = AES256Util.encryptWithIV(requestDto.getNumber());
        Optional<User> checkNumber = userRepository.findByNumber(encryptedNumber);
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

        // 사용자 이름, 주소 암호화
        String encryptedUserName = AES256Util.encryptWithIV(requestDto.getUserName());
        String encryptedAddress = AES256Util.encryptWithIV(requestDto.getAddress());

        // 사용자 등록
        User user = User.builder()
                .address(encryptedAddress)
                .email(encryptedEmail)
                .number(encryptedNumber)
                .password(password)
                .role(role)
                .userName(encryptedUserName).build();
        userRepository.save(user);
    }


    // 사용자 정보 조회 (복호화)
    public UserInfoDto getUser(Long userId) {
        try {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MadeByException(MadeByErrorCode.USER_NOT_FOUND));

            // 복호화 및 DTO 생성
            return UserInfoDto.builder()
                    .email(AES256Util.decryptWithIV(user.getEmail()))
                    .number(AES256Util.decryptWithIV(user.getNumber()))
                    .userName(AES256Util.decryptWithIV(user.getUserName()))
                    .address(user.getAddress())
                    .build();
        } catch (MadeByException e) {
            // MadeByException 그대로 전달
            throw e;
        } catch (Exception e) {
            // 복호화 실패 시 DECRYPTION_ERROR 반환
            throw new MadeByException(MadeByErrorCode.DECRYPTION_ERROR);
        }
    }


}
