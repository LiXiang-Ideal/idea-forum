package com.devcommunity.api.dto;

import com.devcommunity.api.entity.CommentEntity;
import com.devcommunity.api.entity.CommentRow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneOffset;

public final class CommentDtos {
    private CommentDtos() {
    }

    public record CreateRequest(@NotBlank @Size(max = 3000) String body) {
    }

    public record CommentView(Long id, String authorName, String body, Instant createdAt) {
        public static CommentView from(CommentRow row) {
            return new CommentView(row.getId(), row.getAuthorName(), row.getBody(),
                    row.getCreatedAt().toInstant(ZoneOffset.UTC));
        }

        public static CommentView from(CommentEntity comment, String authorName) {
            return new CommentView(comment.getId(), authorName, comment.getBody(),
                    comment.getCreatedAt().toInstant(ZoneOffset.UTC));
        }
    }
}
