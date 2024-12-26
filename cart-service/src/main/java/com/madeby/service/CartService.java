package com.madeby.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.dto.CartResponseDto;
import com.madeby.entity.Cart;
import com.madeby.entity.ProductInfo;
import com.madeby.entity.User;
import com.madeby.repository.CartRepository;
import com.madeby.repository.ProductInfoRepository;
import com.madeby.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductInfoRepository productInfoRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    private static final String CART_CACHE_KEY_PREFIX = "cart:";
    private static final Duration CACHE_TTL = Duration.ofDays(2); // 2일

    private String getCacheKey(Long userId) {
        return CART_CACHE_KEY_PREFIX + userId;
    }

    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long userId) {
        String cacheKey = getCacheKey(userId);
        // 캐시에서 확인
        CartResponseDto cachedCartDto = (CartResponseDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCartDto != null) {
            return cachedCartDto;
        }

        // Redis에 캐시가 없으면 DB에서 조회 또는 새로 생성
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        // Redis 캐시에 저장
        CartResponseDto cartDto = CartResponseDto.fromEntity(cart);
        saveCartToCache(cacheKey, cartDto);
        return cartDto;
    }

    @Transactional
    public void addProduct(Long userId, Long productInfoId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        ProductInfo productInfo = getProductInfo(productInfoId);
        cart.addProduct(productInfo, quantity);
        cartRepository.save(cart);

        // JPQL로 수정 시간 갱신
        cartRepository.updateModifiedAt(cart.getId(), LocalDateTime.now());

        updateCartCache(userId, cart);
    }

    @Transactional
    public void removeProduct(Long userId, Long productInfoId) {
        Cart cart = getOrCreateCart(userId); // 장바구니 조회 또는 생성
        ProductInfo productInfo = getProductInfo(productInfoId); // ProductInfo 조회
        cart.removeProduct(productInfo); // ProductInfo 기반으로 상품 제거
        cartRepository.save(cart);

        // 캐시 갱신
        updateCartCache(userId, cart);
    }

    @Transactional
    public void updateProductQuantity(Long userId, Long productInfoId, int quantity) {
        Cart cart = getOrCreateCart(userId); // 장바구니 조회 또는 생성
        ProductInfo productInfo = getProductInfo(productInfoId); // ProductInfo 조회
        cart.updateQuantity(productInfo, quantity); // ProductInfo 기반으로 수량 업데이트
        cartRepository.save(cart);

        // 캐시 갱신
        updateCartCache(userId, cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.USER_NOT_FOUND));
        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setCartProducts(new ArrayList<>()); // 명시적 초기화
        return cartRepository.save(newCart); // 바로 저장
    }

    private ProductInfo getProductInfo(Long productInfoId) {
        return productInfoRepository.findById(productInfoId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));
    }

    private void saveCartToCache(String cacheKey, CartResponseDto cartDto) {
        if (cartDto != null) {
            redisTemplate.opsForValue().set(cacheKey, cartDto, CACHE_TTL);
        }
    }

    private void updateCartCache(Long userId, Cart cart) {
        String cacheKey = getCacheKey(userId);
        CartResponseDto cartDto = CartResponseDto.fromEntity(cart);
        saveCartToCache(cacheKey, cartDto);
    }
}