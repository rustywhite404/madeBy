package com.madeby.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.dto.SignupRequestDto;
import com.madeby.dto.UserInfoDto;
import com.madeby.entity.User;
import com.madeby.entity.UserRoleEnum;
import com.madeby.repository.UserRepository;
import com.madeby.util.AES256Util;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;

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

        // 비교용 해시 생성
        String emailHash = AES256Util.hashEmail(requestDto.getEmail());

        // 핸드폰번호 중복확인
        String encryptedNumber = AES256Util.encryptWithIV(requestDto.getNumber());
        Optional<User> checkNumber = userRepository.findByNumber(encryptedNumber);
        if (checkNumber.isPresent()) {
            throw new MadeByException(MadeByErrorCode.DUPLICATED_NUMBER);
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (requestDto.isAdmin()) {
            ADMIN_TOKEN = "rustywhite404admin";
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
                .emailHash(emailHash)
                .number(encryptedNumber)
                .password(password)
                .role(role)
                .userName(encryptedUserName).build();
        userRepository.save(user);

        //인증코드 생성
        String verificationCode = UUID.randomUUID().toString().substring(0, 6);

        // Redis에 저장 (5분 만료)
        redisTemplate.opsForValue().set(
                "email:verification:" + user.getId(),
                verificationCode,
                Duration.ofMinutes(5)
        );

        // 인증 메일 발송
        sendVerificationEmail(user.getId(), requestDto.getEmail(), verificationCode);
    }

    // 회원가입 후 인증 메일 발송
    private void sendVerificationEmail(Long id, String email, String verificationCode) throws MessagingException {
        String subject = "MadeBy 가입 완료를 위해 인증을 완료해주세요.";
        String message = "<!DOCTYPE html>" +
                "<html>" +
                "<head><title>Email 인증</title></head>" +
                "<body>" +
                "<h2>이 링크를 누르면 인증이 완료됩니다</h2>" +
                "<form action='http://localhost:8080/api/user/auth/" + id + "/" + verificationCode + "' method='POST'>" +
                "<button type='submit' style='background-color:#4CAF50; border:none; color:white; padding:10px 20px; text-align:center; " +
                "text-decoration:none; display:inline-block; font-size:16px; margin:10px 0; cursor:pointer; border-radius:5px;'>Verify</button>" +
                "</form>" +
                "</body>" +
                "</html>";

        Dotenv dotenv = Dotenv.load();
        String fromEmail = dotenv.get("FROM_EMAIL");

        // MimeMessage 생성
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // MimeMessageHelper를 사용해 HTML 이메일 설정
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
        helper.setFrom(fromEmail); // 발신자 이메일
        helper.setTo(email); // 수신자 이메일
        helper.setSubject(subject); // 이메일 제목
        helper.setText(message, true); // 두 번째 매개변수를 true로 설정해 HTML 지원

        // 이메일 발송
        mailSender.send(mimeMessage);

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


    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        // 필요한 필드만 로드
        User currentUser = userRepository.findByIdWithoutOrders(user.getId())
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.USER_NOT_FOUND));

        // 비밀번호 변경 로직
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new MadeByException(MadeByErrorCode.INVALID_PASSWORD);
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));

        // Refresh Token 삭제
        String redisKey = "refreshToken:" + user.getEmailHash();
        if (redisTemplate.hasKey(redisKey)) {
            redisTemplate.delete(redisKey);
        }

        // 엔티티 저장 시 orders 로딩 방지
        userRepository.saveAndFlush(currentUser); // 즉시 플러시
    }
}
