package com.ltqtest.springbootquickstart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Value("${file.upload.base-path}")
    private String uploadBasePath;
    
    @Value("${file.upload.avatar-path}")
    private String avatarPath;
    
    @Value("${file.upload.access-base-url}")
    private String accessBaseUrl;
    
  
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射头像文件路径
        // 例如：访问 http://localhost:8080/avatars/123/avatar_123_uuid.jpg 会映射到 C:/uploads/avatars/123/avatar_123_uuid.jpg
        registry.addResourceHandler("/" + avatarPath + "**")
                .addResourceLocations("file:" + uploadBasePath + avatarPath);
    }
}