package com.devcommunity.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.AuthDtos;
import com.devcommunity.api.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    @Test
    void registrationAlwaysCreatesARegularUserWithHashedPassword() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secure-pass")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(42L);
            return 1;
        });
        AuthService service = new AuthService(userMapper, passwordEncoder);

        UserEntity response = service.register(
                new AuthDtos.RegisterRequest("Writer", "WRITER@example.com", "secure-pass", "Writer Name"));

        assertEquals("writer", response.getUsername());
        verify(passwordEncoder).encode("secure-pass");
        verify(userMapper).insert(org.mockito.ArgumentMatchers.<UserEntity>argThat(
                user -> "bcrypt-hash".equals(user.getPasswordHash()) && "USER".equals(user.getRole())));
    }

    @Test
    void registrationRejectsPasswordsOverBcryptByteLimit() {
        AuthService service = new AuthService(mock(UserMapper.class), mock(PasswordEncoder.class));
        String multibytePassword = "密".repeat(25);

        assertThrows(DomainException.class, () -> service.register(
                new AuthDtos.RegisterRequest("writer", "writer@example.com", multibytePassword, "Writer")));
    }

    @Test
    void loginAcceptsUsernameOrEmailAndReturnsTheAuthenticatedUser() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserEntity user = new UserEntity();
        user.setUsername("writer");
        user.setPasswordHash("bcrypt-hash");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("secure-pass", "bcrypt-hash")).thenReturn(true);
        AuthService service = new AuthService(userMapper, passwordEncoder);

        UserEntity result = service.login(new AuthDtos.LoginRequest("Writer", "secure-pass"));

        assertEquals("writer", result.getUsername());
    }

    @Test
    void loginRejectsInvalidCredentials() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserEntity user = new UserEntity();
        user.setPasswordHash("bcrypt-hash");
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong-pass", "bcrypt-hash")).thenReturn(false);
        AuthService service = new AuthService(userMapper, passwordEncoder);

        assertThrows(DomainException.class,
                () -> service.login(new AuthDtos.LoginRequest("writer", "wrong-pass")));
    }
}
