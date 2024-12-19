package com.madeby.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoDto {
    private final String userName;
    private final String email;
    private final String number;
    private final String address;
    private final boolean isAdmin;

    // 최소 정보 리턴
    public UserInfoDto(String userName, boolean isAdmin) {
        this.userName = userName;
        this.isAdmin = isAdmin;
        this.email = null;
        this.number = null;
        this.address = null;
    }

    // 전체 정보 리턴
    public UserInfoDto(String userName, String email, String number, String address, boolean isAdmin) {
        this.userName = userName;
        this.email = email;
        this.number = number;
        this.address = address;
        this.isAdmin = isAdmin;
    }
}