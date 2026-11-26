package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.CommentMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.CommentDtos;
import com.devcommunity.api.entity.ArticleEntity;
import com.devcommunity.api.entity.CommentEntity;
import com.devcommunity.api.entity.UserEntity;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    public CommentService(ArticleMapper articleMapper, CommentMapper commentMapper, UserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    public List<CommentDtos.CommentView> list(long articleId) {
        requirePublishedArticle(articleId);
        return commentMapper.findActiveByArticle(articleId, 100).stream()
                .map(CommentDtos.CommentView::from)
                .toList();
    }

    @Transactional
    public CommentDtos.CommentView create(long articleId, String username, String body) {
        requirePublishedArticle(articleId);
        UserEntity author = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getStatus, "ACTIVE"));
        if (author == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "用户不存在或已停用");
        }

        CommentEntity comment = new CommentEntity();
        comment.setArticleId(articleId);
        comment.setAuthorId(author.getId());
        comment.setBody(body.trim());
        comment.setStatus("ACTIVE");
        comment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        commentMapper.insert(comment);
        return CommentDtos.CommentView.from(comment, author.getDisplayName());
    }

    private ArticleEntity requirePublishedArticle(long articleId) {
        ArticleEntity article = articleMapper.selectById(articleId);
        if (article == null || !"PUBLISHED".equals(article.getStatus())) {
            throw new DomainException(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", "文章不存在");
        }
        return article;
    }
}
