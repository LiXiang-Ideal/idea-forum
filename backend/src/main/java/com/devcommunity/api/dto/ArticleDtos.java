package com.devcommunity.api.dto;

import com.devcommunity.api.entity.ArticleRow;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public final class ArticleDtos {
    private ArticleDtos() {
    }

    public record WriteRequest(
            @NotNull @Size(max = 200) String title,
            @Size(max = 500) String summary,
            @NotNull @Size(max = 200000) String body,
            Long categoryId,
            @Pattern(regexp = "DRAFT|PUBLISHED", message = "状态只能为 DRAFT 或 PUBLISHED")
            String status) {
    }

    public record ArticleView(
            Long id,
            String title,
            String summary,
            String body,
            String authorName,
            String categoryName,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            Instant publishedAt,
            Long categoryId,
            String status) {
        public static ArticleView from(ArticleRow row) {
            return new ArticleView(row.getId(), row.getTitle(), row.getSummary(), row.getBody(),
                    row.getAuthorName(), row.getCategoryName(), row.getViewCount(), row.getLikeCount(),
                    row.getCommentCount(),
                    row.getPublishedAt() == null ? null : row.getPublishedAt().toInstant(ZoneOffset.UTC),
                    row.getCategoryId(), row.getStatus());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }
}
