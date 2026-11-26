package com.devcommunity.api.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AvatarWebConfiguration implements WebMvcConfigurer {
    private final String avatarResourceLocation;

    public AvatarWebConfiguration(
            @Value("${app.upload.avatar-directory:uploads/avatars}") String avatarDirectory) {
        String location = Path.of(avatarDirectory).toAbsolutePath().normalize().toUri().toString();
        this.avatarResourceLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/uploads/avatars/**")
                .addResourceLocations(avatarResourceLocation);
    }
}
