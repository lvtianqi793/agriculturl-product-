package com.ltqtest.springbootquickstart.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shopping_cart")
@EntityListeners(AuditingEntityListener.class)
public class ShoppingCart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id", nullable = false, updatable = false, insertable = false)
    private Integer cartId;
    
    @Column(name = "product_id", nullable = false)
    private Integer productId;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "amount", nullable = false)
    private Integer amount;
    
    @Column(name = "total_price", nullable = false)
    private Double totalPrice;
    
    @Column(name = "get_address", length = 200)
    private String getAddress;
    
    @Column(name = "status", columnDefinition = "TINYINT default 1")
    private Integer status = 1; // 1-未结算，2-已结算，3-已取消
    
    @CreatedDate
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}