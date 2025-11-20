package com.ltqtest.springbootquickstart.repository;

import com.ltqtest.springbootquickstart.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    
    /**
     * 根据购买ID查询购买记录
     * @param purchaseId 购买记录ID
     * @return 购买记录
     */
    Purchase findByPurchaseId(Integer purchaseId);
    
    /**
     * 根据用户ID查询购买记录列表
     * @param userId 用户ID
     * @return 购买记录列表
     */
    List<Purchase> findByUserId(Integer userId);
    
    /**
     * 根据用户ID和状态查询购买记录
     * @param userId 用户ID
     * @param status 订单状态
     * @return 购买记录列表
     */
    List<Purchase> findByUserIdAndStatus(Integer userId, Integer status);
}