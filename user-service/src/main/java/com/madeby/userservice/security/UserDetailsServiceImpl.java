package com.madeby.userservice.security;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeBy.shared.util.AES256Util;
import com.madeby.userservice.entity.User;
import com.madeby.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String emailHash = null;
        // 입력된 email이 이미 해시된 값인지 확인
        if (email.contains("@")) { // 평문 이메일일 경우
            try {
                emailHash = AES256Util.hashEmail(email);
            } catch (Exception e) {
                throw new RuntimeException("이메일 해시 처리 중 오류 발생", e);
            }
        } else { // 이미 해시된 값일 경우
            emailHash = email;
        }
        // 사용자 조회
        User user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.USER_NOT_FOUND));

        // 이메일 인증 확인
        if (!user.isEmailVerified()) {
            log.info("이메일 인증되지 않은 사용자 로그인 시도: {}", email);
            throw new MadeByException(MadeByErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 탈퇴 여부 확인
        if (user.isDeleted()) {
            log.info("탈퇴된 사용자 로그인 시도: {}", email);
            throw new MadeByException(MadeByErrorCode.USER_DELETED);
        }

        return new UserDetailsImpl(user);
    }

}