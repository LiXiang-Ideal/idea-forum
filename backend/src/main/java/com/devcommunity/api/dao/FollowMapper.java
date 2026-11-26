package com.devcommunity.api.dao;

import com.devcommunity.api.entity.UserSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FollowMapper {
    @Select("""
            SELECT COUNT(*)
            FROM follows f
            JOIN users u ON u.id = f.follower_id
            WHERE f.followee_id = #{userId} AND u.status = 'ACTIVE'
            """)
    long countFollowers(@Param("userId") long userId);

    @Select("""
            SELECT COUNT(*)
            FROM follows f
            JOIN users u ON u.id = f.followee_id
            WHERE f.follower_id = #{userId} AND u.status = 'ACTIVE'
            """)
    long countFollowing(@Param("userId") long userId);

    @Select("""
            SELECT COUNT(*)
            FROM follows
            WHERE follower_id = #{followerId} AND followee_id = #{followeeId}
            """)
    int exists(
            @Param("followerId") long followerId,
            @Param("followeeId") long followeeId);

    @Insert("""
            INSERT INTO follows (follower_id, followee_id)
            VALUES (#{followerId}, #{followeeId})
            """)
    int add(
            @Param("followerId") long followerId,
            @Param("followeeId") long followeeId);

    @Delete("""
            DELETE FROM follows
            WHERE follower_id = #{followerId} AND followee_id = #{followeeId}
            """)
    int remove(
            @Param("followerId") long followerId,
            @Param("followeeId") long followeeId);

    @Select("""
            SELECT u.id, u.username, u.display_name, u.avatar_url, u.bio
            FROM follows f
            JOIN users u ON u.id = f.follower_id
            WHERE f.followee_id = #{userId} AND u.status = 'ACTIVE'
            ORDER BY f.created_at DESC, u.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<UserSummaryRow> findFollowers(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Select("""
            SELECT u.id, u.username, u.display_name, u.avatar_url, u.bio
            FROM follows f
            JOIN users u ON u.id = f.followee_id
            WHERE f.follower_id = #{userId} AND u.status = 'ACTIVE'
            ORDER BY f.created_at DESC, u.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<UserSummaryRow> findFollowing(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);
}
