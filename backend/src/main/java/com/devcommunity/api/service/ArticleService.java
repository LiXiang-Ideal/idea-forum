package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.CategoryMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.entity.ArticleEntity;
import com.devcommunity.api.entity.ArticleRow;
import com.devcommunity.api.entity.CategoryEntity;
import com.devcommunity.api.entity.UserEntity;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleService {
    private static final int MAX_PAGE_SIZE = 50;

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public ArticleService(ArticleMapper articleMapper, CategoryMapper categoryMapper, UserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    public ArticleDtos.PageResponse<ArticleDtos.ArticleView> list(
            int page, int size, String keyword, Long categoryId) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 100) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_SEARCH", "搜索关键词不能超过 100 个字符");
        }
        List<ArticleDtos.ArticleView> items = articleMapper
                .findPublished(normalizedKeyword, categoryId, normalizedSize, offset)
                .stream()
                .map(ArticleDtos.ArticleView::from)
                .toList();
        long total = articleMapper.countPublished(normalizedKeyword, categoryId);
        return new ArticleDtos.PageResponse<>(items, normalizedPage, normalizedSize, total);
    }

    @Transactional
    public ArticleDtos.ArticleView get(long articleId, String username, boolean isAdmin) {
        ArticleEntity entity = articleMapper.selectById(articleId);
        if (entity == null || "ARCHIVED".equals(entity.getStatus())) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
        boolean published = "PUBLISHED".equals(entity.getStatus());
        if (!published && (username == null || !canEdit(entity, username, isAdmin))) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
        if (published) {
            articleMapper.incrementViewCount(articleId);
        }
        ArticleRow row = articleMapper.findArticleDetail(articleId);
        if (row == null) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
        if (published && row.getViewCount() != null) {
            row.setViewCount(row.getViewCount() + 1);
        }
        return ArticleDtos.ArticleView.from(row);
    }

    @Transactional
    public ArticleDtos.ArticleView create(ArticleDtos.WriteRequest request, String username) {
        String status = normalizeStatus(request.status());
        String title = request.title().trim();
        String body = request.body().trim();
        validateContent(status, title, body);
        UserEntity author = findActiveUser(username);
        validateCategory(request.categoryId());
        ArticleEntity entity = new ArticleEntity();
        entity.setAuthorId(author.getId());
        entity.setCategoryId(request.categoryId());
        entity.setTitle(title);
        entity.setSummary(request.summary() == null ? "" : request.summary().trim());
        entity.setBody(body);
        entity.setStatus(status);
        entity.setViewCount(0L);
        if ("PUBLISHED".equals(entity.getStatus())) {
            entity.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        articleMapper.insert(entity);
        return ArticleDtos.ArticleView.from(articleMapper.findArticleDetail(entity.getId()));
    }

    @Transactional
    public ArticleDtos.ArticleView update(
            long articleId, ArticleDtos.WriteRequest request, String username, boolean isAdmin) {
        ArticleEntity entity = articleMapper.selectById(articleId);
        if (entity == null || "ARCHIVED".equals(entity.getStatus())) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
        if (!canEdit(entity, username, isAdmin)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权修改此文章");
        }
        String status = normalizeStatus(request.status());
        String title = request.title().trim();
        String body = request.body().trim();
        validateContent(status, title, body);
        validateCategory(request.categoryId());
        boolean isBeingPublished = "PUBLISHED".equals(status) && !"PUBLISHED".equals(entity.getStatus());
        entity.setCategoryId(request.categoryId());
        entity.setTitle(title);
        entity.setSummary(request.summary() == null ? "" : request.summary().trim());
        entity.setBody(body);
        entity.setStatus(status);
        if (isBeingPublished) {
            entity.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        articleMapper.updateArticle(entity);
        return ArticleDtos.ArticleView.from(articleMapper.findArticleDetail(articleId));
    }

    @Transactional
    public void archive(long articleId, String username, boolean isAdmin) {
        ArticleEntity entity = articleMapper.selectById(articleId);
        if (entity == null || "ARCHIVED".equals(entity.getStatus())) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
        if (!canEdit(entity, username, isAdmin)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权删除此文章");
        }
        entity.setStatus("ARCHIVED");
        articleMapper.updateById(entity);
    }

    private UserEntity findActiveUser(String username) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "用户不存在或已停用");
        }
        return user;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CategoryEntity category = categoryMapper.selectById(categoryId);
        if (category == null || !"ACTIVE".equals(category.getStatus())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", "所选分类不可用");
        }
    }

    private boolean canEdit(ArticleEntity entity, String username, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        UserEntity user = username == null ? null : userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
        return user != null && entity.getAuthorId().equals(user.getId());
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "DRAFT" : status.trim().toUpperCase(Locale.ROOT);
        if (!"DRAFT".equals(normalized) && !"PUBLISHED".equals(normalized)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "文章状态无效");
        }
        return normalized;
    }

    private void validateContent(String status, String title, String body) {
        if ("PUBLISHED".equals(status) && (title.isBlank() || body.isBlank())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_ARTICLE", "发布文章时标题和正文不能为空");
        }
    }
}
