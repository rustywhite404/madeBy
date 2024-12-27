package com.madeby.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.madeby.orderservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDto {
    private Long id;
    private String userName;
    private String email;
    private String number;
    private String address;
    @JsonProperty("admin") // JSON 필드를 "admin"으로 매핑
    private boolean isAdmin;

    // 별도의 정적 팩토리 메서드로 최소 정보 생성
    public static UserInfoDto minimalInfo(String userName, boolean isAdmin) {
        return UserInfoDto.builder()
                .userName(userName)
                .isAdmin(isAdmin)
                .build();
    }

    // Entity를 DTO로 변환
    public static UserInfoDto fromEntity(User user) {
        return UserInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .number(user.getNumber())
                .address(user.getAddress())
                .isAdmin(user.getRole().equals("ADMIN")) // 권한 체크
                .build();
    }
}
