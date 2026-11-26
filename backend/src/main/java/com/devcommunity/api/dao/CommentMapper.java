package com.devcommunity.api.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devcommunity.api.entity.CommentEntity;
import com.devcommunity.api.entity.CommentRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CommentMapper extends BaseMapper<CommentEntity> {
    @Select("""
            SELECT c.id, u.display_name AS author_name, c.body, c.created_at
            FROM comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.article_id = #{articleId} AND c.status = 'ACTIVE'
            ORDER BY c.created_at ASC, c.id ASC
            LIMIT #{limit}
            """)
    List<CommentRow> findActiveByArticle(
            @Param("articleId") long articleId,
            @Param("limit") int limit);

    @Select("""
            SELECT c.id, c.article_id, a.title AS article_title, c.body, c.created_at
            FROM comments c
            JOIN articles a ON a.id = c.article_id
            WHERE c.author_id = #{userId} AND c.status = 'ACTIVE' AND a.status = 'PUBLISHED'
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CommentRow> findActiveByAuthor(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
            FROM comments c
            JOIN articles a ON a.id = c.article_id
            WHERE c.author_id = #{userId} AND c.status = 'ACTIVE' AND a.status = 'PUBLISHED'
            """)
    long countActiveByAuthor(@Param("userId") long userId);
}
