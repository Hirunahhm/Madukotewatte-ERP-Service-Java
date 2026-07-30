package com.madukotawatte.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.madukotawatte.erp.dto.auth.ChangePasswordRequest;
import com.madukotawatte.erp.dto.auth.LoginRequest;
import com.madukotawatte.erp.dto.auth.LoginResponse;
import com.madukotawatte.erp.dto.auth.RegisterRequest;
import com.madukotawatte.erp.dto.auth.UserResponse;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.security.SecurityConfig;
import com.madukotawatte.erp.security.UserPrincipal;
import com.madukotawatte.erp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final String BASE_URL = "/api/v1/auth";

    private UserPrincipal adminPrincipal;
    private UserPrincipal supervisorPrincipal;
    private UserResponse sampleUserResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(
                "user-uuid-001", "admin", "admin@estate.com", "hashedpw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        supervisorPrincipal = new UserPrincipal(
                "user-uuid-002", "supervisor", "sup@estate.com", "hashedpw",
                List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR")));

        sampleUserResponse = UserResponse.builder()
                .userId("user-uuid-001")
                .username("admin")
                .email("admin@estate.com")
                .role("ROLE_ADMIN")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/login
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("valid credentials → 200 with token")
        void validCredentials_returns200() throws Exception {
            LoginResponse loginResponse = LoginResponse.builder()
                    .token("eyJhbGci.sample.jwt.token")
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .username("admin")
                    .role("ROLE_ADMIN")
                    .build();
            when(authService.login(any())).thenReturn(loginResponse);

            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            mockMvc.perform(post(BASE_URL + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("eyJhbGci.sample.jwt.token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("blank username and password → 400")
        void blankFields_returns400() throws Exception {
            LoginRequest invalid = new LoginRequest(); // username and password null → @NotBlank fails

            mockMvc.perform(post(BASE_URL + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("login is public — no token needed")
        void isPublicEndpoint_noAuthRequired() throws Exception {
            when(authService.login(any())).thenReturn(LoginResponse.builder()
                    .token("tok").tokenType("Bearer").expiresIn(3600L)
                    .username("admin").role("ROLE_ADMIN").build());

            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("password123");

            // No @WithMockUser, no .with(user(...)) — should still reach the endpoint
            mockMvc.perform(post(BASE_URL + "/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/register
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        private RegisterRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new RegisterRequest();
            validRequest.setUsername("newuser");
            validRequest.setEmail("newuser@estate.com");
            validRequest.setPassword("password123");
            validRequest.setRole("ROLE_SUPERVISOR");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin + valid body → 201 with created user")
        void asAdmin_validBody_returns201() throws Exception {
            when(authService.register(any())).thenReturn(sampleUserResponse);

            mockMvc.perform(post(BASE_URL + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value("user-uuid-001"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("username too short / bad email / password too short → 400")
        void invalidBody_returns400() throws Exception {
            RegisterRequest invalid = new RegisterRequest();
            invalid.setUsername("ab");          // min 3
            invalid.setEmail("not-an-email");   // @Email
            invalid.setPassword("short");       // min 8

            mockMvc.perform(post(BASE_URL + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "SUPERVISOR")
        @DisplayName("supervisor → 403")
        void asSupervisor_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post(BASE_URL + "/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/auth/me
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("authenticated admin → 200 with own profile")
        void authenticatedAdmin_returns200() throws Exception {
            when(authService.getCurrentUser("admin")).thenReturn(sampleUserResponse);

            mockMvc.perform(get(BASE_URL + "/me")
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-uuid-001"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.email").value("admin@estate.com"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("authenticated supervisor → 200 with own profile")
        void authenticatedSupervisor_returns200() throws Exception {
            UserResponse supervisorResponse = UserResponse.builder()
                    .userId("user-uuid-002").username("supervisor")
                    .email("sup@estate.com").role("ROLE_SUPERVISOR")
                    .createdAt(LocalDateTime.now())
                    .build();
            when(authService.getCurrentUser("supervisor")).thenReturn(supervisorResponse);

            mockMvc.perform(get(BASE_URL + "/me")
                            .with(user(supervisorPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("supervisor"))
                    .andExpect(jsonPath("$.role").value("ROLE_SUPERVISOR"));
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/auth/change-password
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /auth/change-password")
    class ChangePassword {

        private ChangePasswordRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new ChangePasswordRequest();
            validRequest.setCurrentPassword("oldPassword123");
            validRequest.setNewPassword("newPassword456");
        }

        @Test
        @DisplayName("authenticated + valid body → 204 no content")
        void authenticated_returns204() throws Exception {
            doNothing().when(authService).changePassword(eq("admin"), any());

            mockMvc.perform(put(BASE_URL + "/change-password")
                            .with(csrf())
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("blank current/new password → 400")
        void blankFields_returns400() throws Exception {
            ChangePasswordRequest invalid = new ChangePasswordRequest(); // null fields → @NotBlank fails

            mockMvc.perform(put(BASE_URL + "/change-password")
                            .with(csrf())
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("new password too short → 400")
        void shortNewPassword_returns400() throws Exception {
            ChangePasswordRequest invalid = new ChangePasswordRequest();
            invalid.setCurrentPassword("oldPassword123");
            invalid.setNewPassword("short"); // min 8

            mockMvc.perform(put(BASE_URL + "/change-password")
                            .with(csrf())
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/change-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
