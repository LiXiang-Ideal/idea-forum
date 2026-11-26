package com.devcommunity.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devcommunity.api.common.DomainException;
import com.devcommunity.api.dao.ArticleMapper;
import com.devcommunity.api.dao.CategoryMapper;
import com.devcommunity.api.dao.UserMapper;
import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.entity.ArticleEntity;
import com.devcommunity.api.entity.ArticleRow;
import com.devcommunity.api.entity.UserEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArticleServiceTest {
    private ArticleMapper articleMapper;
    private UserMapper userMapper;
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        userMapper = mock(UserMapper.class);
        articleService = new ArticleService(articleMapper, mock(CategoryMapper.class), userMapper);
    }

    @Test
    void clampsPageSizeAndTrimsSearchTerm() {
        when(articleMapper.findPublished("java", 4L, 50, 50L)).thenReturn(List.of());
        when(articleMapper.countPublished("java", 4L)).thenReturn(0L);

        ArticleDtos.PageResponse<ArticleDtos.ArticleView> result =
                articleService.list(2, 100, " java ", 4L);

        assertEquals(2, result.page());
        assertEquals(50, result.size());
        assertEquals(0L, result.total());
        verify(articleMapper).findPublished("java", 4L, 50, 50L);
    }

    @Test
    void rejectsOverlongSearchTerm() {
        assertThrows(DomainException.class, () -> articleService.list(1, 10, "x".repeat(101), null));
    }

    @Test
    void savesDraftWithBlankTitleAndBody() {
        UserEntity author = new UserEntity();
        author.setId(7L);
        author.setStatus("ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(author);
        when(articleMapper.insert(any(ArticleEntity.class))).thenAnswer(invocation -> {
            ((ArticleEntity) invocation.getArgument(0)).setId(15L);
            return 1;
        });
        ArticleRow savedRow = new ArticleRow();
        savedRow.setId(15L);
        savedRow.setTitle("");
        savedRow.setBody("");
        when(articleMapper.findArticleDetail(15L)).thenReturn(savedRow);

        ArticleDtos.ArticleView result = articleService.create(
                new ArticleDtos.WriteRequest("", "", "", null, "DRAFT"), "writer");

        assertEquals("", result.title());
        assertEquals("", result.body());
        verify(articleMapper).insert(any(ArticleEntity.class));
    }

    @Test
    void rejectsPublishingWithoutBody() {
        assertThrows(DomainException.class, () -> articleService.create(
                new ArticleDtos.WriteRequest("Title", "", "  ", null, "PUBLISHED"), "writer"));
    }

    @Test
    void updatesOwnedDraftWithoutCreatingAnotherArticle() {
        ArticleEntity draft = ownedDraft();
        when(articleMapper.selectById(5L)).thenReturn(draft);
        when(userMapper.selectOne(any())).thenReturn(activeAuthor());
        ArticleRow updatedRow = new ArticleRow();
        updatedRow.setId(5L);
        updatedRow.setTitle("Revised draft");
        updatedRow.setBody("Draft body");
        updatedRow.setStatus("DRAFT");
        when(articleMapper.findArticleDetail(5L)).thenReturn(updatedRow);

        ArticleDtos.ArticleView result = articleService.update(
                5L, new ArticleDtos.WriteRequest("Revised draft", "", "Draft body", null, "DRAFT"),
                "writer", false);

        assertEquals(5L, result.id());
        assertEquals("DRAFT", result.status());
        verify(articleMapper).updateArticle(draft);
        verify(articleMapper, never()).insert(any(ArticleEntity.class));
    }

    @Test
    void republishesOwnedDraftByUpdatingItsExistingId() {
        ArticleEntity draft = ownedDraft();
        when(articleMapper.selectById(5L)).thenReturn(draft);
        when(userMapper.selectOne(any())).thenReturn(activeAuthor());
        ArticleRow publishedRow = new ArticleRow();
        publishedRow.setId(5L);
        publishedRow.setTitle("Published draft");
        publishedRow.setBody("Draft body");
        publishedRow.setStatus("PUBLISHED");
        when(articleMapper.findArticleDetail(5L)).thenReturn(publishedRow);

        ArticleDtos.ArticleView result = articleService.update(
                5L, new ArticleDtos.WriteRequest("Published draft", "", "Draft body", null, "PUBLISHED"),
                "writer", false);

        assertEquals(5L, result.id());
        assertEquals("PUBLISHED", result.status());
        verify(articleMapper).updateArticle(draft);
        verify(articleMapper, never()).insert(any(ArticleEntity.class));
    }

    @Test
    void returnsCategoryAndStatusWhenOpeningOwnedDraft() {
        ArticleEntity draft = ownedDraft();
        draft.setCategoryId(3L);
        when(articleMapper.selectById(5L)).thenReturn(draft);
        when(userMapper.selectOne(any())).thenReturn(activeAuthor());
        ArticleRow draftRow = new ArticleRow();
        draftRow.setId(5L);
        draftRow.setCategoryId(3L);
        draftRow.setStatus("DRAFT");
        when(articleMapper.findArticleDetail(5L)).thenReturn(draftRow);

        ArticleDtos.ArticleView result = articleService.get(5L, "writer", false);

        assertEquals(3L, result.categoryId());
        assertEquals("DRAFT", result.status());
    }

    @Test
    void hidesAnotherUsersDraft() {
        ArticleEntity draft = new ArticleEntity();
        draft.setId(5L);
        draft.setAuthorId(10L);
        draft.setStatus("DRAFT");
        when(articleMapper.selectById(5L)).thenReturn(draft);
        UserEntity otherUser = new UserEntity();
        otherUser.setId(11L);
        when(userMapper.selectOne(any())).thenReturn(otherUser);

        assertThrows(DomainException.class, () -> articleService.get(5L, "other", false));
    }

    private ArticleEntity ownedDraft() {
        ArticleEntity draft = new ArticleEntity();
        draft.setId(5L);
        draft.setAuthorId(7L);
        draft.setStatus("DRAFT");
        return draft;
    }

    private UserEntity activeAuthor() {
        UserEntity author = new UserEntity();
        author.setId(7L);
        author.setStatus("ACTIVE");
        return author;
    }
}
