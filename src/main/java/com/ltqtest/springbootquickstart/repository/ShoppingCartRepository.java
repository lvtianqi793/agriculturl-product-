package com.ltqtest.springbootquickstart.repository;

import com.ltqtest.springbootquickstart.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByCartId(Long cartId);
    List<ShoppingCart> findByUserId(Long userId);
    List<ShoppingCart> findByUserIdAndStatus(Long userId, Integer status);
}