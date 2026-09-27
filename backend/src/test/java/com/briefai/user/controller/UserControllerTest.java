package com.briefai.user.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.config.SecurityConfig;
import com.briefai.security.service.JwtService;
import com.briefai.user.dto.CurrentUserResponse;
import com.briefai.user.repository.UserRepository;
import com.briefai.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getCurrentUser_shouldReturn403WithoutAuthentication() throws Exception {

        mockMvc.perform(get("/api/v1/user/loggedInUser"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getCurrentUser_shouldReturn200ForAuthenticatedUser() throws Exception {

        AuthenticatedUser principal = new AuthenticatedUser(1L, "penny@test.com");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, null);
        when(userService.getCurrentUser(1L)).thenReturn(new CurrentUserResponse(1L, "Penny", "penny@test.com"));

        mockMvc.perform(get("/api/v1/user/loggedInUser")
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Penny"))
                .andExpect(jsonPath("$.email").value("penny@test.com"));

        verify(userService).getCurrentUser(1L);
    }
}