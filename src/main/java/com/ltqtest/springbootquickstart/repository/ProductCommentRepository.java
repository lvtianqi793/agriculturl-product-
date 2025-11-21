package com.ltqtest.springbootquickstart.repository;

import com.ltqtest.springbootquickstart.entity.ProductComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCommentRepository extends JpaRepository<ProductComment, Long> {
    
    // 根据评论ID查询评论
    Optional<ProductComment> findByProductCommentId(Long productCommentId);
    
    // 根据父评论ID查询所有子评论
    List<ProductComment> findByRootCommentId(Long rootCommentId);

    // 根据用户ID和评论ID查询评论（用于验证用户权限）
    Optional<ProductComment> findByProductCommentIdAndUserId(Long productCommentId, Integer userId);
    
    // 根据商品ID查询所有评论
    List<ProductComment> findByProductId(Integer productId);
    
    // 根据商品ID查询rootCommentId为null的顶级评论
    List<ProductComment> findByProductIdAndRootCommentIdIsNull(Integer productId);
}