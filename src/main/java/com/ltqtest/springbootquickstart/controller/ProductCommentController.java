package com.ltqtest.springbootquickstart.controller;

import com.ltqtest.springbootquickstart.entity.ProductComment;
import com.ltqtest.springbootquickstart.repository.ProductCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ltqtest.springbootquickstart.common.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/comment")
public class ProductCommentController {
    
    @Autowired
    private ProductCommentRepository productCommentRepository;
    
    /**
     * 点赞功能接口
     * 不需要记录用户ID，只需要增加对应的点赞次数
     */
    @PostMapping("/like")
    public Result<Map<String, Long>> likeComment(Long productCommentId) {
        if (productCommentId == null) {
            return Result.error(400, "评论ID不能为空");
        }
        
        // 查找评论
        Optional<ProductComment> commentOpt = productCommentRepository.findByProductCommentId(productCommentId);
        if (!commentOpt.isPresent()) {
            return Result.error(404, "评论不存在");
        }
        
        // 增加点赞数
        ProductComment comment = commentOpt.get();
        Long currentLikeCount = comment.getCommentLikeCount();
        comment.setCommentLikeCount(currentLikeCount != null ? currentLikeCount + 1 : 1);
        productCommentRepository.save(comment);
        
        // 返回最新的点赞数
        Map<String, Long> data = new HashMap<>();
        data.put("commentLikeCount", comment.getCommentLikeCount());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", data);
        return Result.success(200, "点赞成功", data);
    }
    /**
     * 删除评论功能接口
     * 1. 用户只能删除自己的评论
     * 2. 删除评论时需要级联删除所有子评论
     */
    @PostMapping("/delete")
    public Result<Map<String, Object>> deleteComment(Long productCommentId,Integer userId) {
        if (productCommentId == null || userId == null) {
            return Result.error(400, "评论ID和用户ID不能为空");
        }
        
        // 验证用户是否有权限删除该评论
        Optional<ProductComment> commentOpt = productCommentRepository.findByProductCommentIdAndUserId(productCommentId, userId);
        if (!commentOpt.isPresent()) {
            // 检查评论是否存在
            Optional<ProductComment> checkOpt = productCommentRepository.findByProductCommentId(productCommentId);
            if (!checkOpt.isPresent()) {
                return Result.error(404, "评论不存在");
            } else {
                // 评论存在但不属于当前用户
                return Result.error(403, "您无权删除该评论");
            }
        }
        
        // 执行级联删除操作
        deleteCommentAndChildren(productCommentId);
        
        // 返回成功响应
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "删除成功");
        
        return Result.success(200, "删除成功", response);
    }
    
    // 递归删除评论及其子评论
    private void deleteCommentAndChildren(Long commentId) {
        // 查找当前评论
        Optional<ProductComment> commentOpt = productCommentRepository.findByProductCommentId(commentId);
        if (commentOpt.isPresent()) {
            // 首先查找所有引用此评论的子评论
            // 1. 查找所有以当前评论为root_comment_id的子评论
            List<ProductComment> childCommentsByRoot = productCommentRepository.findByRootCommentId(commentId);
            // 2. 查找所有以当前评论为to_comment_id的回复评论
            List<ProductComment> childCommentsByTo = productCommentRepository.findByToCommentId(commentId);
            
            // 合并所有子评论
            List<ProductComment> allChildComments = new ArrayList<>();
            allChildComments.addAll(childCommentsByRoot);
            allChildComments.addAll(childCommentsByTo);
            
            // 递归删除所有子评论
            for (ProductComment child : allChildComments) {
                deleteCommentAndChildren(child.getProductCommentId());
            }
            
            // 最后删除当前评论
            productCommentRepository.delete(commentOpt.get());
        }
    }
   
}
