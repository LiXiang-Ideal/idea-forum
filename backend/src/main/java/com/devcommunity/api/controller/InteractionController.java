package com.devcommunity.api.controller;

import com.devcommunity.api.dto.InteractionDtos;
import com.devcommunity.api.service.InteractionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/articles/{articleId}")
public class InteractionController {
    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping("/interactions")
    public InteractionDtos.InteractionView state(
            @PathVariable long articleId, Authentication authentication) {
        return interactionService.state(articleId, authenticatedUsername(authentication));
    }

    @PutMapping("/like")
    public InteractionDtos.InteractionView like(
            @PathVariable long articleId, Authentication authentication) {
        return interactionService.setLike(articleId, authentication.getName(), true);
    }

    @DeleteMapping("/like")
    public InteractionDtos.InteractionView unlike(
            @PathVariable long articleId, Authentication authentication) {
        return interactionService.setLike(articleId, authentication.getName(), false);
    }

    @PutMapping("/bookmark")
    public InteractionDtos.InteractionView bookmark(
            @PathVariable long articleId, Authentication authentication) {
        return interactionService.setBookmark(articleId, authentication.getName(), true);
    }

    @DeleteMapping("/bookmark")
    public InteractionDtos.InteractionView unbookmark(
            @PathVariable long articleId, Authentication authentication) {
        return interactionService.setBookmark(articleId, authentication.getName(), false);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
