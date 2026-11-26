package com.devcommunity.api.dto;

public final class InteractionDtos {
    private InteractionDtos() {
    }

    public record InteractionView(
            boolean liked,
            boolean bookmarked,
            long likeCount,
            long bookmarkCount) {
    }
}
