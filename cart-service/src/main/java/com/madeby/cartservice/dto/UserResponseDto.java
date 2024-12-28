package com.madeby.cartservice.dto;

import com.madeBy.shared.entity.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id; // 유저 ID
    private String email; // 유저 이메일
    private String userName;
    private UserRoleEnum role;

}