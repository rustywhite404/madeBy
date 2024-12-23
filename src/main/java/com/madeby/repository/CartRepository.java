package com.madeby.repository;

import com.madeby.entity.Cart;
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


}
