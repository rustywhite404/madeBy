package com.madeby.userservice.repository;

import com.madeby.userservice.entity.Cart;
import com.madeby.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Cart c SET c.modifiedAt = :modifiedAt WHERE c.id = :cartId")
    void updateModifiedAt(@Param("cartId") Long cartId, @Param("modifiedAt") LocalDateTime modifiedAt);

    Optional<Cart> findByUser(User user);

    //특정 유저의 장바구니 존재 여부 확인
    boolean existsByUser(User user);

    //특정 장바구니에서 상품 제거
    @Modifying
    @Query("DELETE FROM CartProduct cp WHERE cp.cart.id = :cartId AND cp.productInfo.id = :productInfoId")
    int deleteProductFromCart(@Param("cartId") Long cartId, @Param("productInfoId") Long productInfoId);


}
