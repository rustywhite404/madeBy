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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EnvironmentConfig envConfig;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSignupSuccess() throws Exception {
        // Mock 데이터
        SignupRequestDto requestDto = new SignupRequestDto(
                "test@example.com", "password", "testUser","01012341234", "123 Street", false, null);

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByNumber(anyString())).thenReturn(Optional.empty());

        // 호출
        userService.signup(requestDto);

        // 검증
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testSignupDuplicateEmail() throws Exception {
        // Mock 데이터
        SignupRequestDto requestDto = new SignupRequestDto(
                "test@example.com", "password", "testUser","01012341234", "123 Street", false, null);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        // 호출 및 검증
        MadeByException exception = assertThrows(MadeByException.class, () -> userService.signup(requestDto));
        assertEquals(MadeByErrorCode.DUPLICATED_EMAIL, exception.getMadeByErrorCode());
    }

    @Test
    void testSignupDuplicateNumber() throws Exception {
        // Mock 데이터
        SignupRequestDto requestDto = new SignupRequestDto(
                "test@example.com", "password", "testUser","01012341234", "123 Street", false, null);

        when(userRepository.findByNumber(anyString())).thenReturn(Optional.of(new User()));

        // 호출 및 검증
        MadeByException exception = assertThrows(MadeByException.class, () -> userService.signup(requestDto));
        assertEquals(MadeByErrorCode.DUPLICATED_NUMBER, exception.getMadeByErrorCode());
    }

    @Test
    void testGetUserSuccess() throws Exception {
        // Mock 데이터
        Long userId = 1L;

        // 실제 암호화된 데이터 생성
        String encryptedEmail = AES256Util.encryptWithIV("test@example.com");
        String encryptedNumber = AES256Util.encryptWithIV("123456789");
        String encryptedUserName = AES256Util.encryptWithIV("testUser");

        User mockUser = User.builder()
                .email(encryptedEmail)
                .number(encryptedNumber)
                .userName(encryptedUserName)
                .address("encryptedAddress") // 주소는 암호화되지 않은 것으로 가정
                .role(UserRoleEnum.USER)
                .build();

        // Mock 동작 설정
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // 호출
        UserInfoDto userInfoDto = userService.getUser(userId);

        // 검증
        assertEquals("test@example.com", userInfoDto.getEmail());
        assertEquals("123456789", userInfoDto.getNumber());
        assertEquals("testUser", userInfoDto.getUserName());
        assertEquals("encryptedAddress", userInfoDto.getAddress());
    }


    @Test
    void testGetUserNotFound() {
        // Mock 데이터
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // 호출 및 검증
        MadeByException exception = assertThrows(MadeByException.class, () -> userService.getUser(userId));
        assertEquals(MadeByErrorCode.USER_NOT_FOUND, exception.getMadeByErrorCode());
    }
}
