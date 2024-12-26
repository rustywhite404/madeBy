package com.madeby.entity;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Cart extends Timestamped{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartProduct> cartProducts = new ArrayList<>();

    // 상품 추가
    public boolean addProduct(ProductInfo productInfo, int quantity) {
        for (CartProduct cartProduct : cartProducts) {
            if (cartProduct.getProductInfo().equals(productInfo)) {
                cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
                return true; // 상품 수량 업데이트
            }
        }
        this.cartProducts.add(new CartProduct(this, productInfo, quantity));
        return true; // 새 상품 추가
    }

    // 상품 삭제
    public void removeProduct(ProductInfo productInfo) {
        this.cartProducts.removeIf(cartProduct -> cartProduct.getProductInfo().equals(productInfo));
    }

    // 수량 업데이트
    public void updateQuantity(ProductInfo productInfo, int quantity) {
        for (CartProduct cartProduct : cartProducts) {
            if (cartProduct.getProductInfo().equals(productInfo)) {
                cartProduct.setQuantity(quantity);
                return; // 업데이트 완료 후 종료
            }
        }
        throw new MadeByException(MadeByErrorCode.CART_PRODUCT_NOT_FOUND);
    }

}