package com.devcommunity.api.dto;

import com.devcommunity.api.entity.ArticleRow;
import com.devcommunity.api.entity.CommentRow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneOffset;

public final class ProfileDtos {
    private ProfileDtos() {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 80) String displayName,
            @Size(max = 500) String avatarUrl,
            @Size(max = 300) String bio) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword,
            @NotBlank @Size(max = 72) String confirmNewPassword) {
    }

    public record ArticleActivity(
            Long id,
            String title,
            String summary,
            String authorName,
            String authorUsername,
            String categoryName,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            Instant publishedAt,
            String status) {
        public static ArticleActivity from(ArticleRow row) {
            return new ArticleActivity(
                    row.getId(),
                    row.getTitle(),
                    row.getSummary(),
                    row.getAuthorName(),
                    row.getAuthorUsername(),
                    row.getCategoryName(),
                    row.getViewCount(),
                    row.getLikeCount(),
                    row.getCommentCount(),
                    row.getPublishedAt() == null ? null : row.getPublishedAt().toInstant(ZoneOffset.UTC),
                    row.getStatus());
        }
    }

    public record CommentActivity(
            Long id,
            Long articleId,
            String articleTitle,
            String body,
            Instant createdAt) {
        public static CommentActivity from(CommentRow row) {
            return new CommentActivity(
                    row.getId(),
                    row.getArticleId(),
                    row.getArticleTitle(),
                    row.getBody(),
                    row.getCreatedAt().toInstant(ZoneOffset.UTC));
        }
    }
}
