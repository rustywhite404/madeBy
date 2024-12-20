package com.madeby.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MadeByErrorCode {

    DECRYPTION_ERROR("복호화에 실패했습니다."),
    ENCRYPTION_ERROR("복호화에 실패했습니다."),
    OUT_OF_RANGE("요청 범위를 초과했습니다."),
    NO_PRODUCT("상품이 존재하지 않습니다."),
    NO_WISHLIST("위시리스트에 등록된 상품이 없습니다."),
    DUPLICATED_USER("해당 정보로 가입된 사용자가 이미 존재합니다."),
    DUPLICATED_EMAIL("이미 사용중인 이메일입니다."),
    DUPLICATED_NUMBER("이미 사용중인 핸드폰 번호입니다."),
    DUPLICATED_WISHLIST("이미 위시리스트에 등록된 상품입니다."),
    WRONG_ADMIN_TOKEN("잘못된 관리자 암호입니다. 올바른 관리자 암호로 가입을 시도해주세요."),
    INTERNAL_SERVER_ERROR("서버에 오류가 발생했습니다."),
    INVALID_REQUEST("잘못된 요청입니다"),
    REDIS_ROCK_ERROR("레디스 락 처리 중 문제가 발생했습니다."),
    RESOURCE_LOCK_FAILURE("리소스 잠금에 실패했습니다."),
    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다."),
    AUTHENTICATION_FAILED("인증에 실패했습니다."),
    USER_DELETED("탈퇴한 사용자입니다.");

    private final String message;

}
