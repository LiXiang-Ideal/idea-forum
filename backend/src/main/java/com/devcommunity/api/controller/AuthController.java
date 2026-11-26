package com.devcommunity.api.controller;

import com.devcommunity.api.dto.AuthDtos;
import com.devcommunity.api.entity.UserEntity;
import com.devcommunity.api.security.SessionAuthenticationService;
import com.devcommunity.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public AuthController(AuthService authService, SessionAuthenticationService sessionAuthenticationService) {
        this.authService = authService;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @GetMapping("/auth/csrf")
    public AuthDtos.CsrfResponse csrfToken(CsrfToken csrfToken) {
        return new AuthDtos.CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/auth/register")
    public AuthDtos.AuthResponse register(
            @Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        UserEntity user = authService.register(request);
        return sessionAuthenticationService.establish(user, servletRequest, servletResponse);
    }

    @PostMapping("/auth/login")
    public AuthDtos.AuthResponse login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        UserEntity user = authService.login(request);
        return sessionAuthenticationService.establish(user, servletRequest, servletResponse);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionAuthenticationService.logout(request, response);
    }

}
