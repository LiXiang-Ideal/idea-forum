package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.CommentMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.dto.AuthDtos;
import com.devcommunity.api.dto.ProfileDtos;
import com.devcommunity.api.entity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    public ProfileService(
            UserMapper userMapper,
            ArticleMapper articleMapper,
            CommentMapper commentMapper,
            PasswordEncoder passwordEncoder,
            AvatarStorageService avatarStorageService) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
    }

    public AuthDtos.UserProfile getProfile(String username) {
        return AuthDtos.UserProfile.from(requireActiveUser(username));
    }

    @Transactional
    public AuthDtos.UserProfile updateProfile(String username, ProfileDtos.UpdateProfileRequest request) {
        UserEntity user = requireActiveUser(username);
        user.setDisplayName(request.displayName().trim());
        user.setAvatarUrl(normalizeOptional(request.avatarUrl()));
        user.setBio(normalizeOptional(request.bio()));
        userMapper.updateById(user);
        return AuthDtos.UserProfile.from(user);
    }

    @Transactional
    public AuthDtos.UserProfile updateAvatar(String username, MultipartFile file) {
        UserEntity user = requireActiveUser(username);
        user.setAvatarUrl(avatarStorageService.store(file));
        userMapper.updateById(user);
        return AuthDtos.UserProfile.from(user);
    }

    @Transactional
    public void changePassword(String username, ProfileDtos.ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRMATION_MISMATCH", "两次输入的新密码不一致");
        }
        validateBcryptPasswordLength(request.currentPassword());
        validateBcryptPasswordLength(request.newPassword());
        UserEntity user = requireActiveUser(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "当前密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    public ArticleDtos.PageResponse<ProfileDtos.ArticleActivity> listArticles(String username, int page, int size) {
        UserEntity user = requireActiveUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<ProfileDtos.ArticleActivity> items = articleMapper
                .findByAuthor(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(ProfileDtos.ArticleActivity::from)
                .toList();
        return new ArticleDtos.PageResponse<>(items, bounds.page(), bounds.size(),
                articleMapper.countByAuthor(user.getId()));
    }

    public ArticleDtos.PageResponse<ProfileDtos.ArticleActivity> listLikes(String username, int page, int size) {
        UserEntity user = requireActiveUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<ProfileDtos.ArticleActivity> items = articleMapper
                .findLikedByUser(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(ProfileDtos.ArticleActivity::from)
                .toList();
        return new ArticleDtos.PageResponse<>(items, bounds.page(), bounds.size(),
                articleMapper.countLikedByUser(user.getId()));
    }

    public ArticleDtos.PageResponse<ProfileDtos.ArticleActivity> listBookmarks(
            String username, int page, int size) {
        UserEntity user = requireActiveUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<ProfileDtos.ArticleActivity> items = articleMapper
                .findBookmarkedByUser(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(ProfileDtos.ArticleActivity::from)
                .toList();
        return new ArticleDtos.PageResponse<>(items, bounds.page(), bounds.size(),
                articleMapper.countBookmarkedByUser(user.getId()));
    }

    public ArticleDtos.PageResponse<ProfileDtos.CommentActivity> listComments(
            String username, int page, int size) {
        UserEntity user = requireActiveUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<ProfileDtos.CommentActivity> items = commentMapper
                .findActiveByAuthor(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(ProfileDtos.CommentActivity::from)
                .toList();
        return new ArticleDtos.PageResponse<>(items, bounds.page(), bounds.size(),
                commentMapper.countActiveByAuthor(user.getId()));
    }

    private UserEntity requireActiveUser(String username) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getStatus, "ACTIVE"));
        if (user == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "用户不存在或已停用");
        }
        return user;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "密码 UTF-8 编码长度不能超过 72 字节");
        }
    }

    private PageBounds normalizePage(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return new PageBounds(normalizedPage, normalizedSize,
                (long) (normalizedPage - 1) * normalizedSize);
    }

    private record PageBounds(int page, int size, long offset) {
    }
}
