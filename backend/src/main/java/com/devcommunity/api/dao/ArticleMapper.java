package com.devcommunity.api.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devcommunity.api.entity.ArticleEntity;
import com.devcommunity.api.entity.ArticleRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ArticleMapper extends BaseMapper<ArticleEntity> {
    @Select("""
            SELECT a.id, a.title, a.summary, NULL AS body,
                   u.display_name AS author_name, c.name AS category_name,
                   a.view_count,
                   (SELECT COUNT(*) FROM article_likes l WHERE l.article_id = a.id) AS like_count,
                   (SELECT COUNT(*) FROM comments cm WHERE cm.article_id = a.id AND cm.status = 'ACTIVE') AS comment_count,
                   a.published_at
            FROM articles a
            JOIN users u ON u.id = a.author_id
            LEFT JOIN categories c ON c.id = a.category_id
            WHERE a.status = 'PUBLISHED'
              AND (#{keyword} = '' OR a.title LIKE CONCAT('%', #{keyword}, '%')
                   OR a.summary LIKE CONCAT('%', #{keyword}, '%')
                   OR a.body LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{categoryId} IS NULL OR a.category_id = #{categoryId})
            ORDER BY a.published_at DESC, a.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ArticleRow> findPublished(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
            FROM articles a
            WHERE a.status = 'PUBLISHED'
              AND (#{keyword} = '' OR a.title LIKE CONCAT('%', #{keyword}, '%')
                   OR a.summary LIKE CONCAT('%', #{keyword}, '%')
                   OR a.body LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{categoryId} IS NULL OR a.category_id = #{categoryId})
            """)
    long countPublished(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    @Select("""
            SELECT a.id, a.title, a.summary, NULL AS body,
                   u.display_name AS author_name, c.name AS category_name,
                   a.view_count,
                   (SELECT COUNT(*) FROM article_likes l WHERE l.article_id = a.id) AS like_count,
                   (SELECT COUNT(*) FROM comments cm WHERE cm.article_id = a.id AND cm.status = 'ACTIVE') AS comment_count,
                   a.published_at, a.status
            FROM articles a
            JOIN users u ON u.id = a.author_id
            LEFT JOIN categories c ON c.id = a.category_id
            WHERE a.author_id = #{userId} AND a.status IN ('DRAFT', 'PUBLISHED')
            ORDER BY a.updated_at DESC, a.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ArticleRow> findByAuthor(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
            FROM articles a
            WHERE a.author_id = #{userId} AND a.status IN ('DRAFT', 'PUBLISHED')
            """)
    long countByAuthor(@Param("userId") long userId);

    @Select("""
            SELECT a.id, a.title, a.summary, NULL AS body,
                   u.display_name AS author_name, c.name AS category_name,
                   a.view_count,
                   (SELECT COUNT(*) FROM article_likes l WHERE l.article_id = a.id) AS like_count,
                   (SELECT COUNT(*) FROM comments cm WHERE cm.article_id = a.id AND cm.status = 'ACTIVE') AS comment_count,
                   a.published_at, a.status
            FROM article_likes l
            JOIN articles a ON a.id = l.article_id
            JOIN users u ON u.id = a.author_id
            LEFT JOIN categories c ON c.id = a.category_id
            WHERE l.user_id = #{userId} AND a.status = 'PUBLISHED'
            ORDER BY l.created_at DESC, a.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ArticleRow> findLikedByUser(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
            FROM article_likes l
            JOIN articles a ON a.id = l.article_id
            WHERE l.user_id = #{userId} AND a.status = 'PUBLISHED'
            """)
    long countLikedByUser(@Param("userId") long userId);

    @Select("""
            SELECT a.id, a.title, a.summary, NULL AS body,
                   u.display_name AS author_name, c.name AS category_name,
                   a.view_count,
                   (SELECT COUNT(*) FROM article_likes l WHERE l.article_id = a.id) AS like_count,
                   (SELECT COUNT(*) FROM comments cm WHERE cm.article_id = a.id AND cm.status = 'ACTIVE') AS comment_count,
                   a.published_at, a.status
            FROM bookmarks b
            JOIN articles a ON a.id = b.article_id
            JOIN users u ON u.id = a.author_id
            LEFT JOIN categories c ON c.id = a.category_id
            WHERE b.user_id = #{userId} AND a.status = 'PUBLISHED'
            ORDER BY b.created_at DESC, a.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ArticleRow> findBookmarkedByUser(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Select("""
            SELECT COUNT(*)
            FROM bookmarks b
            JOIN articles a ON a.id = b.article_id
            WHERE b.user_id = #{userId} AND a.status = 'PUBLISHED'
            """)
    long countBookmarkedByUser(@Param("userId") long userId);

    @Select("""
            SELECT a.id, a.title, a.summary, a.body, a.category_id, a.status,
                   u.display_name AS author_name, c.name AS category_name,
                   a.view_count,
                   (SELECT COUNT(*) FROM article_likes l WHERE l.article_id = a.id) AS like_count,
                   (SELECT COUNT(*) FROM comments cm WHERE cm.article_id = a.id AND cm.status = 'ACTIVE') AS comment_count,
                   a.published_at
            FROM articles a
            JOIN users u ON u.id = a.author_id
            LEFT JOIN categories c ON c.id = a.category_id
            WHERE a.id = #{articleId}
            """)
    ArticleRow findArticleDetail(@Param("articleId") long articleId);

    @Update("UPDATE articles SET view_count = view_count + 1 WHERE id = #{articleId} AND status = 'PUBLISHED'")
    int incrementViewCount(@Param("articleId") long articleId);

    @Update("""
            UPDATE articles
            SET category_id = #{categoryId},
                title = #{title},
                summary = #{summary},
                body = #{body},
                status = #{status},
                published_at = #{publishedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateArticle(ArticleEntity article);
}
