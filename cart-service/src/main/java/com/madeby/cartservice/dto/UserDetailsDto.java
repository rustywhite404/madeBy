package com.madeby.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDetailsDto {
    private Long userId;
    private String emailHash;
    private String role;
    private boolean isEnabled; // 활성화 상태
}
