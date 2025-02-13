package com.madeBy.shared.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MadeByErrorCode {


    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    USER_NOT_AUTH_INFO("사용자 인증 정보가 존재하지 않습니다."),
    USER_NOT_LOGIN("로그인 후 서비스 이용이 가능합니다."),
    EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다."),
    AUTHENTICATION_FAILED("인증에 실패했습니다."),
    USER_DELETED("탈퇴한 사용자입니다."),
    DUPLICATED_USER("해당 정보로 가입된 사용자가 이미 존재합니다."),
    DUPLICATED_EMAIL("이미 사용중인 이메일입니다."),
    DUPLICATED_NUMBER("이미 사용중인 핸드폰 번호입니다."),
    WRONG_ADMIN_TOKEN("잘못된 관리자 암호입니다. 올바른 관리자 암호로 가입을 시도해주세요."),
    INVALID_PASSWORD("잘못된 비밀번호입니다."),
    DUPLICATED_DATA("중복된 데이터가 존재합니다."),

    NO_PRODUCT("상품이 존재하지 않습니다."),
    DUPLICATE_OPTION("이미 동일한 옵션이 존재합니다."),
    NO_SEARCH_RESULT("검색 결과가 존재하지 않습니다."),
    NO_SELLING_PRODUCT("현재 판매중인 상품이 아닙니다."),
    SOLD_OUT("상품이 품절되었습니다."),
    NOT_ENOUGH_PRODUCT("상품이 충분하지 않습니다. 주문 가능 수량을 확인해주세요."),
    NO_WISHLIST("위시리스트에 등록된 상품이 없습니다."),
    DUPLICATED_WISHLIST("이미 위시리스트에 등록된 상품입니다."),
    BUY_FAILED("상품 구매에 실패하였습니다."),

    NO_ORDER("주문 내역이 존재하지 않습니다."),
    DECREMENT_STOCK_FAILURE("상품 구매에 실패했습니다. 재고를 다시 확인해주세요."),
    MIN_AMOUNT("최소 구매 수량은 1개입니다."),
    CART_PRODUCT_NOT_FOUND("장바구니에 포함된 상품이 아닙니다."),
    CART_NOT_FOUND("장바구니에 상품이 존재하지 않습니다."),
    CANNOT_CANCEL_ORDER("배송 시작 전에만 주문을 취소하실 수 있습니다."),
    NOT_YOUR_ORDER("다른 사용자의 주문은 취소하실 수 없습니다."),
    CANNOT_RETURN("배송 완료 상태인 상품만 반품 요청이 가능합니다."),
    RETURN_NOT_ALLOWED("해당 주문은 반품이 불가능합니다. 상세 사유는 고객센터에 문의하세요."),
    STOCK_UPDATE_FAILED("재고 업데이트에 실패했습니다."),
    NO_PAYMENT("해당 주문에 대한 결제 정보를 찾을 수 없습니다."),
    INVALID_STATUS("주문 상태값이 올바르지 않습니다."),
    STATUS_TIMEOUT("주문 상태를 확인하지 못했습니다."),
    

    DECRYPTION_ERROR("복호화에 실패했습니다."),
    ENCRYPTION_ERROR("복호화에 실패했습니다."),
    OUT_OF_RANGE("요청 범위를 초과했습니다."),
    INTERNAL_SERVER_ERROR("서버에 오류가 발생했습니다."),
    INVALID_REQUEST("잘못된 요청입니다"),
    REDIS_ROCK_ERROR("레디스 락 처리 중 문제가 발생했습니다."),
    CONCURRENCY_LOCK_FAILED("락 획득 중 문제가 발생했습니다."),
    RESOURCE_LOCK_FAILURE("리소스 잠금에 실패했습니다."),
    SERVICE_UNAVAILABLE("서비스 장애가 발생했습니다. 나중에 다시 시도해주세요."),
    NO_CACHE("캐시가 존재하지 않습니다.");




    private final String message;

}
