package com.ltqtest.springbootquickstart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.ltqtest.springbootquickstart.common.Result;
import com.ltqtest.springbootquickstart.entity.User;
import com.ltqtest.springbootquickstart.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.logging.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api")
public class UserController {
    
    // 日志记录器
    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    
    @Autowired
    private UserRepository userRepository;
    
    // 不再需要密码加密器
    
    // 从配置文件中读取文件上传路径
    @Value("${file.upload.base-path}")
    private String uploadBasePath;
    
    @Value("${file.upload.avatar-path}")
    private String avatarPath;
    
    @Value("${file.upload.access-base-url}")
    private String accessBaseUrl;
    
    // 支持的图片MIME类型
    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif");
    }
    
    // 根据MIME类型获取文件扩展名
    private String getExtensionByContentType(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            default:
                return "jpg"; // 默认使用jpg扩展名
        }
    }
    
    /**
     * 获取用户个人信息接口
     * 接口路径: GET /api/user/profile
     * @param userId 用户ID
     * @return Result 包含用户个人信息的结果对象
     */
    @GetMapping("/user/profile")
    public Result<Map<String, Object>> getUser(@RequestParam("userId") Integer userId) {
        try {
            // 验证参数
            if(userId == null || userId <= 0) {
                return Result.error(400, "参数错误，userId不能为空");
            }
            // 根据userId查询特定用户，使用Optional处理
            User user = userRepository.findByUserId(userId).orElse(null);
            
            // 检查用户是否存在
            if(user == null) {
                return Result.error(404, "用户不存在");
            }
            
            // 构建响应数据
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("real_name", user.getRealName());
            userData.put("role_type", user.getRoleType());
            userData.put("phone", user.getPhone());
            userData.put("email", user.getEmail());
            userData.put("image_url", "https://example.com/avatar.jpg"); // 添加默认头像URL
            userData.put("expert_id", user.getExpertId() != null ? user.getExpertId() : 0);
            userData.put("approver_id", user.getApproverId() != null ? user.getApproverId() : 0);
            userData.put("create_time", user.getCreateTime());
            return Result.success(200, "成功", userData);
            
        } catch (Exception e) {
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    /**
     * 更新个人信息接口
     * 接口路径: PUT /api/user/profile/update
     * 接口说明：修改当前登录用户的基本信息
     * @param userId 用户ID
     * @param realName 真实姓名
     * @param phone 手机号
     * @param email 邮箱
     * @param imageUrl 头像URL
     * @return Result 更新结果对象
     */
    @PutMapping("/user/profile/update")
    public Result<Map<String, Object>> updateUserProfile(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "real_name", required = false) String realName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "image_url", required = false) String imageUrl) {
        try {
            // 参数验证
            if (userId == null || userId <= 0) {
                return Result.error(400, "参数错误，userId不能为空且必须大于0");
            }
            
           
            // 实现用户信息更新逻辑
            // 1. 根据userId查找用户
            User user = userRepository.findByUserId(userId).orElse(null);
            
            // 2. 检查用户是否存在
            if (user == null) {
                return Result.error(404, "用户不存在");
            }
            
            // 3. 更新用户信息（只更新非空的字段）
            if (realName != null && !realName.isEmpty()) {
                user.setRealName(realName);
            }
            if (phone != null && !phone.isEmpty()) {
                user.setPhone(phone);
            }
            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                user.setImageUrl(imageUrl);
            }
        
            // 5. 保存更新到数据库
            User updatedUser = userRepository.save(user);
            
            // 6. 准备响应数据
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("userId", updatedUser.getUserId());
            resultMap.put("realName", updatedUser.getRealName());
            resultMap.put("phone", updatedUser.getPhone());
            resultMap.put("email", updatedUser.getEmail());
            resultMap.put("imageUrl", updatedUser.getImageUrl());
            
            return Result.success(200, "更新成功", resultMap);
        } catch (IllegalArgumentException e) {
            // 参数无效异常
            return Result.error(400, "参数无效：" + e.getMessage());
        } catch (Exception e) {
            // 记录异常日志
            System.err.println("更新用户信息时发生错误：" + e.getMessage());
            e.printStackTrace();
            
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }
    
    /**
     * 修改密码接口
     * 接口路径: POST /api/user/password/update
     * 接口说明：修改当前用户登录密码（密码以普通字符串形式存储）
     * @param userId 用户ID
     * @param old_password 旧密码
     * @param new_password 新密码
     * @return Result 更新结果对象
     */
    @PostMapping("/user/password/update")
    public Result<?> updatePassword(
            @RequestParam("userId") Integer userId,
            @RequestParam("old_password") String oldPassword,
            @RequestParam("new_password") String newPassword) {
        try {
            // 参数验证和清理
            if (userId == null || userId <= 0) {
                return Result.error(400, "参数错误，userId不能为空且必须大于0");
            }
            
            // 清理和验证旧密码
            oldPassword = StringUtils.trimWhitespace(oldPassword);
            if (oldPassword == null || oldPassword.isEmpty()) {
                return Result.error(400, "旧密码不能为空");
            }
            
            // 清理和验证新密码
            newPassword = StringUtils.trimWhitespace(newPassword);
            if (newPassword == null || newPassword.isEmpty()) {
                return Result.error(400, "新密码不能为空");
            }
            
            // 根据userId查找用户
            User user = userRepository.findByUserId(userId).orElse(null);
            
            // 检查用户是否存在
            if (user == null) {
                return Result.error(404, "用户不存在");
            }
            
            // 获取数据库中存储的密码
            String storedPassword = user.getPassword();
            
            // 直接进行字符串比较验证旧密码
            boolean isOldPasswordCorrect = oldPassword.equals(storedPassword);
            
            // 验证旧密码是否正确
            if (!isOldPasswordCorrect) {
                return Result.error(400, "旧密码不正确");
            }
            
            // 检查新密码是否与旧密码相同
            if (newPassword.equals(oldPassword)) {
                return Result.error(400, "新密码不能与旧密码相同");
            }
            
            // 直接设置原始密码字符串
            user.setPassword(newPassword);
            
            // 保存更新到数据库
            userRepository.save(user);
            
            logger.info("用户ID为" + userId + "的密码修改成功");
            return Result.success(200, "密码修改成功");
        } catch (Exception e) {
            logger.severe("用户ID为" + userId + "的密码修改失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }

    /**
     * 上传头像接口
     * 接口路径: POST /api/user/upload/avatar
     * 接口说明：上传用户头像，返回文件URL
     * @param userId 用户ID
     * @param file 头像文件
     * @return Result 包含头像URL的结果对象
     */
    @PostMapping("/user/upload/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestParam("userId") Integer userId,
            @RequestPart("file") MultipartFile file) {
        
        try {
            // 参数验证
            if (userId == null || userId <= 0) {
                logger.warning("用户ID参数无效: " + userId);
                return Result.error(400, "参数错误，userId不能为空且必须大于0");
            }
            
            if (file == null || file.isEmpty()) {
                logger.warning("用户ID为" + userId + "未选择上传文件");
                return Result.error(400, "请选择要上传的头像文件");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null) {
                logger.warning("用户ID为" + userId + "上传的文件无法识别类型");
                return Result.error(400, "无法识别文件类型");
            }
            
            if (!isValidImageType(contentType)) {
                logger.warning("用户ID为" + userId + "上传了不支持的文件类型: " + contentType);
                return Result.error(400, "只支持JPG、PNG、GIF格式的图片文件上传");
            }
            
            // 查找用户
            User user = userRepository.findByUserId(userId).orElse(null);
            if (user == null) {
                logger.warning("未找到用户ID为" + userId + "的用户信息");
                return Result.error(404, "用户不存在");
            }
            
            // 生成唯一文件名，加入用户ID前缀便于管理
            String originalFilename = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(originalFilename);
            
            // 确保扩展名不为空
            if (extension == null || extension.isEmpty()) {
                // 根据MIME类型推断扩展名
                extension = getExtensionByContentType(contentType);
                logger.info("用户ID为" + userId + "的文件无扩展名，根据MIME类型推断为: " + extension);
            }
            
            String newFilename = "avatar_" + userId + "_" + UUID.randomUUID() + "." + extension;
            
            // 文件保存路径 - 使用基于用户ID的子目录组织文件
            String uploadRootDir = uploadBasePath + avatarPath;
            String userDir = uploadRootDir + userId + "/";
            
            java.io.File dir = new java.io.File(userDir);
            
            // 安全地创建目录，避免路径遍历攻击
            if (!dir.getCanonicalPath().startsWith(new java.io.File(uploadRootDir).getCanonicalPath())) {
                logger.severe("用户ID为" + userId + "尝试使用非法文件路径: " + userDir);
                return Result.error(400, "无效的文件路径");
            }
            
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    logger.severe("无法创建用户ID为" + userId + "的头像存储目录: " + userDir);
                    return Result.error(500, "创建文件存储目录失败");
                }
                logger.info("为用户ID" + userId + "创建了头像存储目录: " + userDir);
            }
            
            // 保存文件
            java.io.File dest = new java.io.File(userDir + newFilename);
            try {
                file.transferTo(dest);
                logger.info("用户ID为" + userId + "的头像文件保存成功: " + dest.getAbsolutePath());
            } catch (Exception e) {
                logger.severe("用户ID为" + userId + "的头像文件保存失败: " + e.getMessage());
                throw e; // 重新抛出异常以便外层捕获
            }
            
            // 生成访问URL
            String imageUrl = accessBaseUrl + avatarPath + userId + "/" + newFilename;
            
            // 记录旧头像URL，用于后续清理
            String oldImageUrl = user.getImageUrl();
            
            // 更新用户头像URL
            user.setImageUrl(imageUrl);
            
            try {
                userRepository.save(user);
                logger.info("成功更新用户ID为" + userId + "的头像URL: " + imageUrl);
            } catch (DataAccessException e) {
                logger.severe("更新用户ID为" + userId + "的头像URL到数据库失败: " + e.getMessage());
                // 尝试删除已上传的文件
                if (dest.exists()) {
                    boolean deleted = dest.delete();
                    logger.info("由于数据库更新失败，" + (deleted ? "成功" : "失败") + "删除已上传的头像文件: " + dest.getAbsolutePath());
                }
                return Result.error(500, "数据库更新失败，请稍后再试");
            }
            
            // 清理旧头像文件
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    deleteOldAvatarFile(oldImageUrl, uploadRootDir, userId);
                } catch (Exception e) {
                    // 清理失败不影响主流程，只记录日志
                    logger.warning("清理用户ID为" + userId + "的旧头像文件失败: " + e.getMessage());
                }
            }
            
            // 构建响应，确保格式符合要求
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("image_url", imageUrl);
            
            logger.info("用户ID为" + userId + "的头像上传请求处理完成");
            return Result.success(200, "上传成功", resultMap);
            
        } catch (IllegalArgumentException e) {
            logger.warning("用户ID为" + userId + "的头像上传参数错误: " + e.getMessage());
            return Result.error(400, "参数错误：" + e.getMessage());
        } catch (java.io.IOException e) {
            logger.severe("用户ID为" + userId + "的头像文件上传失败: " + e.getMessage());
            return Result.error(500, "文件上传失败，请检查文件是否可用");
        } catch (Exception e) {
            logger.severe("用户ID为" + userId + "的头像上传过程中发生未预期错误: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误，请稍后再试");
        }
    }
    
    /**
     * 清理旧头像文件
     * @param oldImageUrl 旧头像URL
     * @param uploadRootDir 上传根目录
     * @param userId 用户ID
     */
    private void deleteOldAvatarFile(String oldImageUrl, String uploadRootDir, Integer userId) {
        try {
            // 从URL中提取文件名
            String fileName = oldImageUrl.substring(oldImageUrl.lastIndexOf("/") + 1);
            if (fileName.contains(userId.toString())) {
                java.io.File oldFile = new java.io.File(uploadRootDir + userId + "/" + fileName);
                if (oldFile.exists() && oldFile.isFile()) {
                    boolean deleted = oldFile.delete();
                    logger.info("" + (deleted ? "成功" : "失败") + "删除用户ID为" + userId + "的旧头像文件: " + oldFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("清理旧头像文件失败", e);
        }
    }

}
