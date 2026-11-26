package com.devcommunity.api.controller;

import com.devcommunity.api.dto.CommentDtos;
import com.devcommunity.api.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentDtos.CommentView> list(@PathVariable long articleId) {
        return commentService.list(articleId);
    }

    @PostMapping
    public CommentDtos.CommentView create(
            @PathVariable long articleId,
            @Valid @RequestBody CommentDtos.CreateRequest request,
            Authentication authentication) {
        return commentService.create(articleId, authentication.getName(), request.body());
    }
}
