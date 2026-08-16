package com.coachkonnects.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.home") + "/coachkonnects_uploads";
        Path uploadDir = Paths.get(uploadPath);
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(uploadDir.toUri().toString() + "/");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/admin/**")
                .addPathPatterns("/api/profile/**")
                .addPathPatterns("/api/enquiries/**")
                .addPathPatterns("/api/classes/**")
                .excludePathPatterns("/api/admin/auth/login")
                .excludePathPatterns("/api/enquiries/send");
    }


}
