package com.devcommunity.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.CommentMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.ProfileDtos;
import com.devcommunity.api.entity.ArticleRow;
import com.devcommunity.api.entity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

class ProfileServiceTest {
    private UserMapper userMapper;
    private ArticleMapper articleMapper;
    private CommentMapper commentMapper;
    private PasswordEncoder passwordEncoder;
    private AvatarStorageService avatarStorageService;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        articleMapper = mock(ArticleMapper.class);
        commentMapper = mock(CommentMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        avatarStorageService = mock(AvatarStorageService.class);
        profileService = new ProfileService(
                userMapper, articleMapper, commentMapper, passwordEncoder, avatarStorageService);
    }

    @Test
    void profileUpdateChangesOnlyEditableFields() {
        UserEntity user = activeUser();
        user.setUsername("writer");
        user.setEmail("writer@example.com");
        when(userMapper.selectOne(any())).thenReturn(user);

        var result = profileService.updateProfile(
                "writer", new ProfileDtos.UpdateProfileRequest("  New Name ", " ", "  About me  "));

        assertEquals("New Name", result.displayName());
        assertNull(result.avatarUrl());
        assertEquals("About me", result.bio());
        assertEquals("writer", user.getUsername());
        assertEquals("writer@example.com", user.getEmail());
        verify(userMapper).updateById(user);
    }

    @Test
    void avatarUploadPersistsTheGeneratedUrlForTheAuthenticatedUser() {
        UserEntity user = activeUser();
        user.setUsername("writer");
        user.setAvatarUrl("https://old.example/avatar.png");
        when(userMapper.selectOne(any())).thenReturn(user);
        var file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "image bytes".getBytes(StandardCharsets.UTF_8));
        when(avatarStorageService.store(file)).thenReturn("/api/v1/uploads/avatars/generated.png");

        var result = profileService.updateAvatar("writer", file);

        assertEquals("/api/v1/uploads/avatars/generated.png", result.avatarUrl());
        verify(avatarStorageService).store(file);
        verify(userMapper).updateById(user);
    }

    @Test
    void rejectsPasswordConfirmationMismatchBeforeCheckingCredentials() {
        assertThrows(DomainException.class, () -> profileService.changePassword(
                "writer", new ProfileDtos.ChangePasswordRequest("old-password", "new-password", "different")));

        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutUpdatingUser() {
        UserEntity user = activeUser();
        user.setPasswordHash("stored-hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(false);

        assertThrows(DomainException.class, () -> profileService.changePassword(
                "writer", new ProfileDtos.ChangePasswordRequest(
                        "old-password", "new-password", "new-password")));

        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    @Test
    void updatesPasswordAfterVerifyingCurrentPassword() {
        UserEntity user = activeUser();
        user.setPasswordHash("stored-hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        profileService.changePassword(
                "writer", new ProfileDtos.ChangePasswordRequest(
                        "old-password", "new-password", "new-password"));

        assertEquals("new-hash", user.getPasswordHash());
        verify(userMapper).updateById(user);
    }

    @Test
    void listsLikesUsingTheAuthenticatedUsersId() {
        UserEntity user = activeUser();
        user.setId(42L);
        when(userMapper.selectOne(any())).thenReturn(user);
        ArticleRow row = new ArticleRow();
        row.setId(15L);
        row.setTitle("Saved article");
        row.setStatus("PUBLISHED");
        when(articleMapper.findLikedByUser(42L, 12, 0L)).thenReturn(List.of(row));
        when(articleMapper.countLikedByUser(42L)).thenReturn(1L);

        var result = profileService.listLikes("writer", 1, 12);

        assertEquals(15L, result.items().get(0).id());
        assertEquals("PUBLISHED", result.items().get(0).status());
        assertEquals(1L, result.total());
        verify(articleMapper).findLikedByUser(42L, 12, 0L);
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setStatus("ACTIVE");
        return user;
    }
}
