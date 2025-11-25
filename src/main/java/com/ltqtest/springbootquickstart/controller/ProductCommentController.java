package com.ltqtest.springbootquickstart.controller;

import com.ltqtest.springbootquickstart.entity.Product;
import com.ltqtest.springbootquickstart.repository.ProductRepository;
import com.ltqtest.springbootquickstart.entity.ProductComment;
import com.ltqtest.springbootquickstart.repository.ProductCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ltqtest.springbootquickstart.common.Result;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/")
public class ProductCommentController {
    
    @Autowired
    private ProductCommentRepository productCommentRepository;
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 点赞功能接口
     * 不需要记录用户ID，只需要增加对应的点赞次数
     */
    @PostMapping("comment/like")
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
    @PostMapping("comment/delete")
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

            // 合并所有子评论
            List<ProductComment> allChildComments = new ArrayList<>();
            allChildComments.addAll(childCommentsByRoot);
            // 递归删除所有子评论
            for (ProductComment child : allChildComments) {
                deleteCommentAndChildren(child.getProductCommentId());
            }
            
            // 最后删除当前评论
            productCommentRepository.delete(commentOpt.get());
        }
    }
    
    /**
     * 用户评论功能接口
     * 添加新评论到数据库
     */
    @PostMapping("comment")
    public Result<Map<String, Object>> addComment(@RequestBody CommentRequest request) {
        try {
            // 参数验证
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return Result.error(400, "评论内容不能为空");
            }
            
            if (request.getUserId() == null || request.getUserId() <= 0) {
                return Result.error(400, "用户ID不能为空且必须大于0");
            }
            
            if (request.getProductId() == null || request.getProductId() <= 0) {
                return Result.error(400, "商品ID不能为空且必须大于0");
            }
            
            // 2. 创建评论对象
            ProductComment comment = new ProductComment();
            comment.setContent(request.getContent());
            // 直接使用当前时间作为发送时间
            comment.setSendTime(LocalDateTime.now());
            comment.setUserId(request.getUserId());
            comment.setProductId(request.getProductId());
            comment.setRootCommentId(request.getRootCommentId());
            comment.setCommentLikeCount(0L);
            comment.setToCommentId(request.getToCommentId());
            // 保存评论到数据库
            productCommentRepository.save(comment);
    
            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("productCommentId", comment.getProductCommentId());
            response.put("content", comment.getContent());
            response.put("rootCommentId", comment.getRootCommentId());
            response.put("sendTime", comment.getSendTime());
            response.put("toCommentId", comment.getToCommentId());
            return Result.success(200, "成功", response);
        } catch (Exception e) {
            // 捕获异常并返回错误响应
            return Result.error(500, "评论保存失败: " + e.getMessage());
        }
    }
    
    // 内部类用于接收请求参数
    private static class CommentRequest {
        private String content;
        // 移除LocalDateTime类型字段，避免格式解析错误
        private Integer userId;
        private Integer productId;
        private Long rootCommentId;
        private Long toCommentId; // 回复的评论id（可为空）
        
        // Getters and Setters
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public Integer getUserId() {
            return userId;
        }
        public void setUserId(Integer userId) {
            this.userId = userId;
        }
        public Integer getProductId() {
            return productId;
        }
        public void setProductId(Integer productId) {
            this.productId = productId;
        }
        public Long getRootCommentId() {
            return rootCommentId;
        }
        public void setRootCommentId(Long rootCommentId) {
            this.rootCommentId = rootCommentId;
        }
        public Long getToCommentId() {
            return toCommentId;
        }
        public void setToCommentId(Long toCommentId) {
            this.toCommentId = toCommentId;
        }
    }
    /**
     * 获取商品所有信息，包括评论
     * 接口路径: GET /api/commentarea
     * @param productId 商品ID
     * @return 商品信息及对应的顶级评论
     */
    @GetMapping("/commentarea")
    public Result<Map<String, Object>> getProductWithComments(@RequestParam Integer productId) {
        try {
            // 验证参数
            if (productId == null || productId <= 0) {
                return Result.error(400, "参数错误：商品ID无效");
            }
            
            // 查询商品信息
            Product product = productRepository.findByProductId(productId);
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            
            // 查询该商品的顶级评论（rootCommentId为null）
            List<ProductComment> comments = productCommentRepository.findByProductIdAndRootCommentIdIsNull(productId);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", product.getUserId());
            responseData.put("productName", product.getProductName());
            responseData.put("price", product.getPrice());
            responseData.put("producer", product.getProducer());
            responseData.put("salesVolumn", product.getSalesVolume());
            responseData.put("productImg", product.getProductImg());
            responseData.put("surplus", product.getSurplus());
            
            // 构建评论列表
            List<Map<String, Object>> commentList = new ArrayList<>();
            for (ProductComment comment : comments) {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("productCommentId", comment.getProductCommentId());
                commentMap.put("content", comment.getContent());
                commentMap.put("sendTime", comment.getSendTime());
                commentMap.put("userId", comment.getUserId());
                commentMap.put("commentLikeCount", comment.getCommentLikeCount()); 
                commentList.add(commentMap);
            }
            
            responseData.put("productComment", commentList);
            
            return Result.success(200, "获取成功", responseData);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

      /**
     * 获取该评论的所有子评论
     * 接口路径: GET /api/comment/childcomment
     * @param product_comment_id 父评论ID
     * @return 子评论列表
     */
    @GetMapping("/comment/childcomment")
    public Result<List<Map<String, Object>>> getChildComments(@RequestParam Long product_comment_id) {
        try {
            // 验证参数
            if (product_comment_id == null || product_comment_id <= 0) {
                return Result.error(400, "参数错误：评论ID无效");
            }
            
            // 查询所有root_comment_id为该product_comment_id的子评论
            List<ProductComment> childComments = productCommentRepository.findByRootCommentId(product_comment_id);
            
            // 构建评论列表
            List<Map<String, Object>> commentList = new ArrayList<>();
            
            // 遍历子评论，构建评论信息
            for (ProductComment comment : childComments) {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("productCommentId", comment.getProductCommentId());
                commentMap.put("toCommentId", comment.getToCommentId());
                commentMap.put("content", comment.getContent());
                commentMap.put("sendTime", comment.getSendTime());
                commentMap.put("userId", comment.getUserId());
                commentMap.put("commentLikeCount", comment.getCommentLikeCount());
                commentList.add(commentMap);
            }
            
            return Result.success(200, "成功返回子评论", commentList);
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
}
