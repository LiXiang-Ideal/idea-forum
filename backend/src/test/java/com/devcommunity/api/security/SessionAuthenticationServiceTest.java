package com.devcommunity.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.devcommunity.api.entity.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class SessionAuthenticationServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishCreatesSessionAndSavesTheAuthenticatedContext() {
        HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
        SessionAuthenticationService service = new SessionAuthenticationService(repository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);
        String oldSessionId = request.getSession(false).getId();

        var result = service.establish(activeUser(), request, response);

        assertEquals("writer", result.user().username());
        assertNotEquals(oldSessionId, request.getSession(false).getId());
        var context = repository.loadDeferredContext(request).get();
        assertEquals("writer", context.getAuthentication().getName());
        assertEquals("ROLE_USER", context.getAuthentication().getAuthorities()
                .iterator().next().getAuthority());
    }

    @Test
    void logoutInvalidatesTheSessionAndClearsAuthentication() {
        HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
        SessionAuthenticationService service = new SessionAuthenticationService(repository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.establish(activeUser(), request, response);

        service.logout(request, response);

        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("writer");
        user.setDisplayName("Writer");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        return user;
    }
}
