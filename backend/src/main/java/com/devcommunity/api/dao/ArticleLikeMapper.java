package com.devcommunity.api.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ArticleLikeMapper {
    @Insert("""
            INSERT IGNORE INTO article_likes (user_id, article_id)
            VALUES (#{userId}, #{articleId})
            """)
    int add(@Param("userId") long userId, @Param("articleId") long articleId);

    @Delete("DELETE FROM article_likes WHERE user_id = #{userId} AND article_id = #{articleId}")
    int remove(@Param("userId") long userId, @Param("articleId") long articleId);

    @Select("SELECT COUNT(*) FROM article_likes WHERE user_id = #{userId} AND article_id = #{articleId}")
    int exists(@Param("userId") long userId, @Param("articleId") long articleId);

    @Select("SELECT COUNT(*) FROM article_likes WHERE article_id = #{articleId}")
    long countByArticle(@Param("articleId") long articleId);
}
