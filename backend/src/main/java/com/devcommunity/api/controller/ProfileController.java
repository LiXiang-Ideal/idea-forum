package com.devcommunity.api.controller;

import com.devcommunity.api.dto.ArticleDtos;
import com.devcommunity.api.dto.AuthDtos;
import com.devcommunity.api.dto.ProfileDtos;
import com.devcommunity.api.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public AuthDtos.UserProfile getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping
    public AuthDtos.UserProfile updateProfile(
            @Valid @RequestBody ProfileDtos.UpdateProfileRequest request,
            Authentication authentication) {
        return profileService.updateProfile(authentication.getName(), request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuthDtos.UserProfile uploadAvatar(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        return profileService.updateAvatar(authentication.getName(), file);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ProfileDtos.ChangePasswordRequest request,
            Authentication authentication) {
        profileService.changePassword(authentication.getName(), request);
    }

    @GetMapping("/articles")
    public ArticleDtos.PageResponse<ProfileDtos.ArticleActivity> articles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return profileService.listArticles(authentication.getName(), page, size);
    }

    @GetMapping("/likes")
    public ArticleDtos.PageResponse<ProfileDtos.ArticleActivity> likes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return profileService.listLikes(authentication.getName(), page, size);
    }

    @GetMapping("/bookmarks")
    public ArticleDtos.PageResponse<ProfileDtos.ArticleActivity> bookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return profileService.listBookmarks(authentication.getName(), page, size);
    }

    @GetMapping("/comments")
    public ArticleDtos.PageResponse<ProfileDtos.CommentActivity> comments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return profileService.listComments(authentication.getName(), page, size);
    }
}
