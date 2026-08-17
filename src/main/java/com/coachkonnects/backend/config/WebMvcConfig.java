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
        exposeDirectory("uploads", registry);
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


    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(System.getProperty("user.home"), "coachkonnects_" + dirName);
        if (dirName.startsWith("../")) {
            dirName = dirName.replace("../", "");
        }
        registry.addResourceHandler("/api/" + dirName + "/**")
                .addResourceLocations("file:" + uploadDir.toAbsolutePath().toString() + "/");
    }
}
