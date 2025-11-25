package com.ltqtest.springbootquickstart.repository;

import com.ltqtest.springbootquickstart.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {
    
    // 根据userId查找用户的所有地址
    List<UserAddress> findByUserId(Integer userId);
    
    // 根据addressId和userId查找特定地址（用于权限验证）
    Optional<UserAddress> findByAddressIdAndUserId(Integer addressId, Integer userId);
}