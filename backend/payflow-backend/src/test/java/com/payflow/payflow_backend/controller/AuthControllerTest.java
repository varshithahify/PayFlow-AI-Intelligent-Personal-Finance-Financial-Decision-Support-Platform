package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.LoginRequest;
import com.payflow.payflow_backend.dto.RegisterRequest;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.service.AuthService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {

        User user = new User(
                "Varshitha",
                "varshitha@example.com",
                "encodedPassword",
                1L
        );

        user.setCreatedAt(LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(user);

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Varshitha",
                                    "email": "varshitha@example.com",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Varshitha"))
                .andExpect(jsonPath("$.email")
                        .value("varshitha@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.orgId").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldRejectRegistrationWhenNameIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "varshitha@example.com",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectRegistrationWhenEmailIsInvalid()
            throws Exception {

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Varshitha",
                                    "email": "invalid-email",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectRegistrationWhenPasswordIsTooShort()
            throws Exception {

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Varshitha",
                                    "email": "varshitha@example.com",
                                    "password": "123"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectRegistrationWhenPasswordIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Varshitha",
                                    "email": "varshitha@example.com"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenReturn("test.jwt.token");

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "varshitha@example.com",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldRejectLoginWhenEmailIsInvalid()
            throws Exception {

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "invalid-email",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectLoginWhenPasswordIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "varshitha@example.com"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectLoginWhenEmailIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectRegistrationWhenServiceThrowsIllegalArgumentException()
            throws Exception {

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Email already registered"
                ));

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Varshitha",
                                    "email": "varshitha@example.com",
                                    "password": "password123"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Email already registered"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldRejectLoginWhenCredentialsAreInvalid()
            throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Invalid email or password"
                ));

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "varshitha@example.com",
                                    "password": "wrongpassword"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid email or password"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldRejectMalformedRegistrationJson()
            throws Exception {

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Varshitha",
                                    "email":
                                """)
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
