package com.devcommunity.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.FollowMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.dto.FollowDtos;
import com.devcommunity.api.entity.UserEntity;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {
    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final ArticleMapper articleMapper;

    public FollowService(UserMapper userMapper, FollowMapper followMapper, ArticleMapper articleMapper) {
        this.userMapper = userMapper;
        this.followMapper = followMapper;
        this.articleMapper = articleMapper;
    }

    @Transactional(readOnly = true)
    public FollowDtos.PublicProfile getPublicProfile(String username, String viewerUsername) {
        UserEntity target = requirePublicUser(username);
        UserEntity viewer = findActiveUser(viewerUsername);
        return publicProfile(target, viewer);
    }

    @Transactional
    public FollowDtos.PublicProfile follow(String actorUsername, String targetUsername) {
        UserEntity actor = requireAuthenticatedUser(actorUsername);
        UserEntity target = requirePublicUser(targetUsername);
        rejectSelfFollow(actor, target);
        try {
            followMapper.add(actor.getId(), target.getId());
        } catch (DuplicateKeyException duplicate) {
            if (followMapper.exists(actor.getId(), target.getId()) == 0) {
                throw duplicate;
            }
        }
        return publicProfile(target, actor);
    }

    @Transactional
    public FollowDtos.PublicProfile unfollow(String actorUsername, String targetUsername) {
        UserEntity actor = requireAuthenticatedUser(actorUsername);
        UserEntity target = requirePublicUser(targetUsername);
        rejectSelfFollow(actor, target);
        followMapper.remove(actor.getId(), target.getId());
        return publicProfile(target, actor);
    }

    @Transactional(readOnly = true)
    public ArticleDtos.PageResponse<ArticleDtos.ArticleView> listPublicArticles(
            String username, int page, int size) {
        UserEntity user = requirePublicUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<ArticleDtos.ArticleView> items = articleMapper
                .findPublishedByAuthor(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(ArticleDtos.ArticleView::from)
                .toList();
        return new ArticleDtos.PageResponse<>(
                items, bounds.page(), bounds.size(), articleMapper.countPublishedByAuthor(user.getId()));
    }

    @Transactional(readOnly = true)
    public ArticleDtos.PageResponse<FollowDtos.UserSummary> listFollowers(
            String username, int page, int size) {
        UserEntity user = requirePublicUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<FollowDtos.UserSummary> items = followMapper
                .findFollowers(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(FollowDtos.UserSummary::from)
                .toList();
        return new ArticleDtos.PageResponse<>(
                items, bounds.page(), bounds.size(), followMapper.countFollowers(user.getId()));
    }

    @Transactional(readOnly = true)
    public ArticleDtos.PageResponse<FollowDtos.UserSummary> listFollowing(
            String username, int page, int size) {
        UserEntity user = requirePublicUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<FollowDtos.UserSummary> items = followMapper
                .findFollowing(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(FollowDtos.UserSummary::from)
                .toList();
        return new ArticleDtos.PageResponse<>(
                items, bounds.page(), bounds.size(), followMapper.countFollowing(user.getId()));
    }

    @Transactional(readOnly = true)
    public ArticleDtos.PageResponse<ArticleDtos.ArticleView> listFeed(String username, int page, int size) {
        UserEntity user = requireAuthenticatedUser(username);
        PageBounds bounds = normalizePage(page, size);
        List<ArticleDtos.ArticleView> items = articleMapper
                .findFollowedFeed(user.getId(), bounds.size(), bounds.offset())
                .stream()
                .map(ArticleDtos.ArticleView::from)
                .toList();
        return new ArticleDtos.PageResponse<>(
                items, bounds.page(), bounds.size(), articleMapper.countFollowedFeed(user.getId()));
    }

    private FollowDtos.PublicProfile publicProfile(UserEntity target, UserEntity viewer) {
        boolean following = viewer != null
                && !viewer.getId().equals(target.getId())
                && followMapper.exists(viewer.getId(), target.getId()) > 0;
        return FollowDtos.PublicProfile.from(
                target,
                followMapper.countFollowers(target.getId()),
                followMapper.countFollowing(target.getId()),
                following);
    }

    private UserEntity requirePublicUser(String username) {
        UserEntity user = findActiveUser(username);
        if (user == null) {
            throw new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在");
        }
        return user;
    }

    private UserEntity requireAuthenticatedUser(String username) {
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

    private void rejectSelfFollow(UserEntity actor, UserEntity target) {
        if (actor.getId().equals(target.getId())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF", "不能关注自己");
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
