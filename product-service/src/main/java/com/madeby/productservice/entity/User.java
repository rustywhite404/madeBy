package com.madeby.productservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User extends Timestamped{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment(value = "이메일 주소(userId를 대체)")
    private String email;

    @Column(nullable = false, unique = true)
    @Comment(value = "로그인 시 비교용 해시값")
    private String emailHash;

    @Column(nullable = false)
    @Comment(value = "이메일 인증여부")
    private boolean emailVerified = false; // 이메일 인증 여부

    @Column(nullable = false)
    @Comment(value = "비밀번호")
    private String password;

    @Column(nullable = false)
    @Comment(value = "유저명")
    private String userName;

    @Column(nullable = false, unique = true)
    @Comment(value = "핸드폰번호")
    private String number; // 핸드폰번호

    @Column(nullable = false)
    @Comment(value = "주소")
    private String address;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Orders> orders = new ArrayList<>();

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Comment(value = "권한")
    private UserRoleEnum role;

    @Column(nullable = false)
    @Comment(value = "탈퇴여부")
    private boolean isDeleted = false; // 회원 탈퇴 여부

    // 생성자 추가
    public User(Long id) {
        this.id = id;
    }

}
