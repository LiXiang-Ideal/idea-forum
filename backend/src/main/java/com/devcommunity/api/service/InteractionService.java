package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleLikeMapper;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.BookmarkMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.InteractionDtos;
import com.devcommunity.api.entity.ArticleEntity;
import com.devcommunity.api.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionService {
    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper likeMapper;
    private final BookmarkMapper bookmarkMapper;
    private final UserMapper userMapper;

    public InteractionService(
            ArticleMapper articleMapper,
            ArticleLikeMapper likeMapper,
            BookmarkMapper bookmarkMapper,
            UserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.likeMapper = likeMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.userMapper = userMapper;
    }

    public InteractionDtos.InteractionView state(long articleId, String username) {
        requirePublishedArticle(articleId);
        UserEntity user = findActiveUser(username);
        long userId = user == null ? -1 : user.getId();
        return new InteractionDtos.InteractionView(
                user != null && likeMapper.exists(userId, articleId) > 0,
                user != null && bookmarkMapper.exists(userId, articleId) > 0,
                likeMapper.countByArticle(articleId),
                bookmarkMapper.countByArticle(articleId));
    }

    @Transactional
    public InteractionDtos.InteractionView setLike(long articleId, String username, boolean enabled) {
        requirePublishedArticle(articleId);
        UserEntity user = requireActiveUser(username);
        if (enabled) {
            likeMapper.add(user.getId(), articleId);
        } else {
            likeMapper.remove(user.getId(), articleId);
        }
        return state(articleId, username);
    }

    @Transactional
    public InteractionDtos.InteractionView setBookmark(long articleId, String username, boolean enabled) {
        requirePublishedArticle(articleId);
        UserEntity user = requireActiveUser(username);
        if (enabled) {
            bookmarkMapper.add(user.getId(), articleId);
        } else {
            bookmarkMapper.remove(user.getId(), articleId);
        }
        return state(articleId, username);
    }

    private void requirePublishedArticle(long articleId) {
        ArticleEntity article = articleMapper.selectById(articleId);
        if (article == null || !"PUBLISHED".equals(article.getStatus())) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
    }

    private UserEntity requireActiveUser(String username) {
        UserEntity user = findActiveUser(username);
        if (user == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "用户不存在或已停用");
        }
        return user;
    }

    private UserEntity findActiveUser(String username) {
        if (username == null) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getStatus, "ACTIVE"));
    }
}
