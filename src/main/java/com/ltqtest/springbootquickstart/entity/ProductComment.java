package com.ltqtest.springbootquickstart.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_comment", schema = "ltq_adep")
public class ProductComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_comment_id", nullable = false, updatable = false)
    private Long productCommentId; // bigint类型
    
    @Column(name = "content", length = 1000, nullable = false)
    private String content; // 评论内容
    
    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime; // 评论时间
    
    @Column(name = "user_id", nullable = false)
    private Integer userId; // 所属用户id
    
    @Column(name = "product_id", nullable = false)
    private Integer productId; // 所属商品id
    
    @Column(name = "comment_like_count")
    private Long commentLikeCount; // 点赞次数（可为空）
    
    @Column(name = "root_comment_id")
    private Long rootCommentId; // 父评论id（可为空）

    @Column(name = "to_comment_id")
    private Long toCommentId; // 回复的评论id（可为空）
    
  
}