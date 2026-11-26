package com.devcommunity.api.dto;

import com.devcommunity.api.entity.UserEntity;
import com.devcommunity.api.entity.UserSummaryRow;

public final class FollowDtos {
    private FollowDtos() {
    }

    public record PublicProfile(
            Long id,
            String username,
            String displayName,
            String avatarUrl,
            String bio,
            long followerCount,
            long followingCount,
            boolean following) {
        public static PublicProfile from(
                UserEntity user, long followerCount, long followingCount, boolean following) {
            return new PublicProfile(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getAvatarUrl(), user.getBio(), followerCount, followingCount, following);
        }
    }

    public record UserSummary(
            Long id,
            String username,
            String displayName,
            String avatarUrl,
            String bio) {
        public static UserSummary from(UserSummaryRow row) {
            return new UserSummary(
                    row.getId(), row.getUsername(), row.getDisplayName(), row.getAvatarUrl(), row.getBio());
        }
    }
}
