package com.madeby.cartservice.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.cartservice.client.ProductServiceClient;
import com.madeby.cartservice.client.UserServiceClient;
import com.madeby.cartservice.dto.CartResponseDto;
import com.madeby.cartservice.dto.ProductInfoDto;
import com.madeby.cartservice.dto.ProductsDto;
import com.madeby.cartservice.entity.Cart;
import com.madeby.cartservice.dto.UserResponseDto;
import com.madeby.cartservice.repository.CartRepository;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;

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
    public void addProduct(Long userId, Long productId, int quantity) {
        // 1. 장바구니 조회 또는 생성
        Cart cart = getOrCreateCart(userId);

        // 2. 상품 정보 검증 (Product-Service 호출)
        ProductsDto product = productServiceClient.getProductById(productId);

        if (!product.isVisible()) {
            throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT); // 상품이 비활성화된 경우
        }

        // 3. 장바구니에 상품 추가
        cart.addProduct(productId, quantity);

        // 4. DB 저장
        cartRepository.save(cart);

        // 5. JPQL로 수정 시간 갱신
        cartRepository.updateModifiedAt(cart.getId(), LocalDateTime.now());

        // 6. Redis 캐시 갱신
        updateCartCache(userId, cart);
    }

    @Transactional
    public void removeProduct(Long userId, Long productId) {
        // 1. 장바구니 조회 또는 생성
        Cart cart = getOrCreateCart(userId);

        // 2. 장바구니에서 상품 제거
        cart.removeProduct(productId); // ProductId 기반으로 제거

        // 3. DB 저장
        cartRepository.save(cart);

        // 4. Redis 캐시 갱신
        updateCartCache(userId, cart);
    }


    @Transactional
    public void updateProductQuantity(Long userId, Long productId, int quantity) {
        // 1. 장바구니 조회 또는 생성
        Cart cart = getOrCreateCart(userId);

        // 2. 수량 업데이트
        cart.updateQuantity(productId, quantity); // productId 기반으로 수량 업데이트

        // 3. DB 저장
        cartRepository.save(cart);

        // 4. Redis 캐시 갱신
        updateCartCache(userId, cart);
    }


    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        validateUser(userId);
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setCartProducts(new ArrayList<>()); // 명시적 초기화
        return cartRepository.save(newCart); // 바로 저장
    }

    private ProductInfoDto getProductInfo(Long productInfoId) {
        ProductInfoDto productInfo = productServiceClient.getProductInfoById(productInfoId);

        if (productInfo == null || productInfo.getStock() <= 0) {
            throw new MadeByException(MadeByErrorCode.NO_PRODUCT); // 상품이 존재하지 않거나 품절인 경우
        }

        return productInfo; // ProductInfoDto 반환
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

    public CartResponseDto getCartByUserId(Long userId) {
        // 1. Redis 캐시 확인
        String cacheKey = getCacheKey(userId);
        CartResponseDto cachedCart = (CartResponseDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCart != null) {
            return cachedCart;
        }

        // 2. 캐시가 없을 경우 DB에서 조회
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        // 3. DTO 변환
        CartResponseDto cartDto = CartResponseDto.fromEntity(cart);

        // 4. Redis에 캐싱
        saveCartToCache(cacheKey, cartDto);

        return cartDto;
    }

    @Transactional
    public void removeProductFromCart(Long userId, Long productId) {
        // 1. DB에서 장바구니 조회
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.CART_NOT_FOUND));

        // 2. 상품 제거
        boolean removed = cart.getCartProducts().removeIf(cartProduct ->
                cartProduct.getProductId().equals(productId));

        if (!removed) {
            throw new MadeByException(MadeByErrorCode.CART_PRODUCT_NOT_FOUND);
        }

        // 3. 장바구니 저장
        cartRepository.save(cart);

        // 4. Redis 캐시 갱신
        updateCartCache(userId, cart);
    }


    @Transactional
    public void clearCart(Long userId) {
        // 1. DB에서 장바구니 조회
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.CART_NOT_FOUND));

        // 2. 장바구니 비우기
        cart.getCartProducts().clear();

        // 3. 장바구니 저장
        cartRepository.save(cart);

        // 4. Redis 캐시 갱신
        updateCartCache(userId, cart);
    }

    private void validateUser(Long userId) {
        UserResponseDto user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new MadeByException(MadeByErrorCode.USER_NOT_FOUND);
        }
    }
}