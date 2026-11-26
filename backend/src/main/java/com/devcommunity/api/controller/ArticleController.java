package com.devcommunity.api.controller;

import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping({"/articles", "/search"})
    public ArticleDtos.PageResponse<ArticleDtos.ArticleView> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return articleService.list(page, size, keyword, categoryId);
    }

    @GetMapping("/articles/{id}")
    public ArticleDtos.ArticleView get(@PathVariable long id, Authentication authentication) {
        return articleService.get(id, username(authentication), isAdmin(authentication));
    }

    @PostMapping("/articles")
    public ArticleDtos.ArticleView create(
            @Valid @RequestBody ArticleDtos.WriteRequest request,
            Authentication authentication) {
        return articleService.create(request, authentication.getName());
    }

    @PutMapping("/articles/{id}")
    public ArticleDtos.ArticleView update(
            @PathVariable long id,
            @Valid @RequestBody ArticleDtos.WriteRequest request,
            Authentication authentication) {
        return articleService.update(id, request, authentication.getName(), isAdmin(authentication));
    }

    @DeleteMapping("/articles/{id}")
    public void archive(@PathVariable long id, Authentication authentication) {
        articleService.archive(id, authentication.getName(), isAdmin(authentication));
    }

    private String username(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
