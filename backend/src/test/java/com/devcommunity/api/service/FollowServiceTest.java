package com.devcommunity.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.FollowMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.entity.ArticleRow;
import com.devcommunity.api.entity.UserEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class FollowServiceTest {
    private UserMapper userMapper;
    private FollowMapper followMapper;
    private ArticleMapper articleMapper;
    private FollowService followService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        followMapper = mock(FollowMapper.class);
        articleMapper = mock(ArticleMapper.class);
        followService = new FollowService(userMapper, followMapper, articleMapper);
    }

    @Test
    void publicProfileContainsCountsAndViewerFollowState() {
        UserEntity target = activeUser(20L, "author");
        UserEntity viewer = activeUser(10L, "reader");
        when(userMapper.selectOne(any())).thenReturn(target, viewer);
        when(followMapper.exists(10L, 20L)).thenReturn(1);
        when(followMapper.countFollowers(20L)).thenReturn(5L);
        when(followMapper.countFollowing(20L)).thenReturn(3L);

        var result = followService.getPublicProfile("author", "reader");

        assertEquals("author", result.username());
        assertEquals(5L, result.followerCount());
        assertEquals(3L, result.followingCount());
        assertTrue(result.following());
    }

    @Test
    void repeatedConcurrentFollowIsIdempotent() {
        UserEntity actor = activeUser(10L, "reader");
        UserEntity target = activeUser(20L, "author");
        when(userMapper.selectOne(any())).thenReturn(actor, target);
        doThrow(new DuplicateKeyException("duplicate")).when(followMapper).add(10L, 20L);
        when(followMapper.exists(10L, 20L)).thenReturn(1);
        when(followMapper.countFollowers(20L)).thenReturn(1L);
        when(followMapper.countFollowing(20L)).thenReturn(0L);

        var result = followService.follow("reader", "author");

        assertTrue(result.following());
        assertEquals(1L, result.followerCount());
    }

    @Test
    void unfollowRemovesTheRelationshipAndReturnsUpdatedState() {
        UserEntity actor = activeUser(10L, "reader");
        UserEntity target = activeUser(20L, "author");
        when(userMapper.selectOne(any())).thenReturn(actor, target);
        when(followMapper.remove(10L, 20L)).thenReturn(1);
        when(followMapper.exists(10L, 20L)).thenReturn(0);
        when(followMapper.countFollowers(20L)).thenReturn(0L);
        when(followMapper.countFollowing(20L)).thenReturn(0L);

        var result = followService.unfollow("reader", "author");

        assertFalse(result.following());
        assertEquals(0L, result.followerCount());
        verify(followMapper).remove(10L, 20L);
    }

    @Test
    void rejectsSelfFollowBeforeWritingRelationship() {
        UserEntity actor = activeUser(10L, "reader");
        when(userMapper.selectOne(any())).thenReturn(actor, actor);

        assertThrows(DomainException.class, () -> followService.follow("reader", "reader"));

        verifyNoInteractions(followMapper);
    }

    @Test
    void hidesInactiveProfileAsNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(DomainException.class, () -> followService.getPublicProfile("disabled", null));

        verifyNoInteractions(followMapper);
    }

    @Test
    void feedClampsPageSizeAndUsesOnlyTheAuthenticatedUserId() {
        UserEntity reader = activeUser(10L, "reader");
        ArticleRow row = new ArticleRow();
        row.setId(42L);
        row.setAuthorUsername("author");
        when(userMapper.selectOne(any())).thenReturn(reader);
        when(articleMapper.findFollowedFeed(10L, 50, 50L)).thenReturn(List.of(row));
        when(articleMapper.countFollowedFeed(10L)).thenReturn(1L);

        var result = followService.listFeed("reader", 2, 100);

        assertEquals(2, result.page());
        assertEquals(50, result.size());
        assertEquals("author", result.items().get(0).authorUsername());
        assertEquals(1L, result.total());
        verify(articleMapper).findFollowedFeed(10L, 50, 50L);
    }

    @Test
    void anonymousPublicProfileDoesNotSetFollowing() {
        UserEntity target = activeUser(20L, "author");
        when(userMapper.selectOne(any())).thenReturn(target);
        when(followMapper.countFollowers(20L)).thenReturn(0L);
        when(followMapper.countFollowing(20L)).thenReturn(0L);

        var result = followService.getPublicProfile("author", null);

        assertFalse(result.following());
        verify(followMapper, never()).exists(20L, 20L);
    }

    private UserEntity activeUser(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setStatus("ACTIVE");
        return user;
    }
}
