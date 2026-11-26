package com.devcommunity.api.controller;

import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.dto.FollowDtos;
import com.devcommunity.api.service.FollowService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FollowController {
    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @GetMapping("/profiles/{username}")
    public FollowDtos.PublicProfile profile(
            @PathVariable String username, Authentication authentication) {
        return followService.getPublicProfile(username, authenticatedUsername(authentication));
    }

    @GetMapping("/profiles/{username}/articles")
    public ArticleDtos.PageResponse<ArticleDtos.ArticleView> articles(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return followService.listPublicArticles(username, page, size);
    }

    @GetMapping("/profiles/{username}/followers")
    public ArticleDtos.PageResponse<FollowDtos.UserSummary> followers(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return followService.listFollowers(username, page, size);
    }

    @GetMapping("/profiles/{username}/following")
    public ArticleDtos.PageResponse<FollowDtos.UserSummary> following(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return followService.listFollowing(username, page, size);
    }

    @PutMapping("/profiles/{username}/follow")
    public FollowDtos.PublicProfile follow(
            @PathVariable String username, Authentication authentication) {
        return followService.follow(authentication.getName(), username);
    }

    @DeleteMapping("/profiles/{username}/follow")
    public FollowDtos.PublicProfile unfollow(
            @PathVariable String username, Authentication authentication) {
        return followService.unfollow(authentication.getName(), username);
    }

    @GetMapping("/users/me/feed")
    public ArticleDtos.PageResponse<ArticleDtos.ArticleView> feed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return followService.listFeed(authentication.getName(), page, size);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
