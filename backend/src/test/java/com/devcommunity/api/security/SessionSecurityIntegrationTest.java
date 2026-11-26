package com.devcommunity.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devcommunity.api.controller.AuthController;
import com.devcommunity.api.controller.ProfileController;
import com.devcommunity.api.dto.AuthDtos;
import com.devcommunity.api.entity.UserEntity;
import com.devcommunity.api.service.AuthService;
import com.devcommunity.api.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = SessionSecurityIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class SessionSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void loginRequiresCsrfAndPersistsAuthenticationInTheSession() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        AuthDtos.CsrfResponse csrf = objectMapper.readValue(
                csrfResult.getResponse().getContentAsString(), AuthDtos.CsrfResponse.class);
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        when(authService.login(any())).thenReturn(activeUser());
        when(profileService.getProfile("writer")).thenReturn(AuthDtos.UserProfile.from(activeUser()));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .header(csrf.headerName(), csrf.token())
                        .contentType("application/json")
                        .content("{\"username\":\"writer\",\"password\":\"secure-pass\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/users/me").session(authenticatedSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(authenticatedSession)
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());
        assertTrue(authenticatedSession.isInvalid());
    }

    @Test
    void loginWithoutCsrfTokenIsRejectedBeforeCredentialValidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"writer\",\"password\":\"secure-pass\"}"))
                .andExpect(status().isForbidden());

        verify(authService, never()).login(any());
    }

    @Test
    void protectedProfileEndpointRejectsRequestsWithoutASession() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
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

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class})
    @Import({AuthController.class, ProfileController.class, SecurityConfiguration.class,
            SessionAuthenticationService.class})
    static class TestApplication {
    }
}
