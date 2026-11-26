package com.devcommunity.api.dto;

import com.devcommunity.api.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名需为 3-32 位字母、数字或下划线")
            String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 1, max = 80) String displayName) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record UserProfile(Long id, String username, String displayName, String avatarUrl, String bio) {
        public static UserProfile from(UserEntity user) {
            return new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getAvatarUrl(), user.getBio());
        }
    }

    public record AuthResponse(UserProfile user) {
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
