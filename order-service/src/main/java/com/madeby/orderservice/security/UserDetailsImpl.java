package com.madeby.orderservice.security;

import com.madeby.orderservice.entity.User;
import com.madeby.orderservice.entity.UserRoleEnum;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    // User 엔티티 반환 (필요 시 추가 정보에 접근 가능)
    public User getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // username 대신 email 반환
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    // 탈퇴 여부 확인
    public boolean isDeleted() {
        return user.isDeleted();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRoleEnum role = user.getRole();
        String authority = role.getAuthority();

        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(authority);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(simpleGrantedAuthority);

        return authorities;
    }

    // 계정 활성화 여부 (이메일 인증 및 탈퇴 여부 확인)
    @Override
    public boolean isEnabled() {
        return user.isEmailVerified() && !user.isDeleted();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠금 여부 (탈퇴 여부를 기준으로 처리)
    @Override
    public boolean isAccountNonLocked() {
        return !user.isDeleted();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
