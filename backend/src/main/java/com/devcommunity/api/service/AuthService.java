package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.AuthDtos;
import com.devcommunity.api.entity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity register(AuthDtos.RegisterRequest request) {
        validateBcryptPasswordLength(request.password());
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Long existing = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .or()
                .eq(UserEntity::getEmail, email));
        if (existing != null && existing > 0) {
            throw new DomainException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS", "用户名或邮箱已被使用");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return user;
    }

    public UserEntity login(AuthDtos.LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        String identifier = request.username().trim().toLowerCase(Locale.ROOT);
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .and(query -> query.eq(UserEntity::getUsername, identifier)
                        .or()
                        .eq(UserEntity::getEmail, identifier))
                .last("LIMIT 1"));
        if (user == null || !"ACTIVE".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        return user;
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "密码 UTF-8 编码长度不能超过 72 字节");
        }
    }
}
