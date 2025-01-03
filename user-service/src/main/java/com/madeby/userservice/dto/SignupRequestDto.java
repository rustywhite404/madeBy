package com.madeby.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String userName;

    @NotBlank
    private String number;

    @NotBlank
    private String address;

    private boolean admin = false;
    private String adminToken = "";
}
