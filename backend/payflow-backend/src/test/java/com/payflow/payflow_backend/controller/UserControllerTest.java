package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.UserRequest;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
controllers = UserController.class,
excludeFilters = @ComponentScan.Filter(
type = FilterType.ASSIGNABLE_TYPE,
classes = JwtAuthenticationFilter.class
)
)
@AutoConfigureMockMvc
@Import(UserControllerTest.TestSecurityConfig.class)
class UserControllerTest {
@Autowired
private MockMvc mockMvc;

@MockitoBean
private UserService userService;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
static class TestSecurityConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )

                        .accessDeniedHandler((request, response, ex) ->
                                response.sendError(
                                        HttpStatus.FORBIDDEN.value()
                                )
                        )
                );

        return http.build();
    }
}

@Test
void adminShouldCreateUser() throws Exception {

    when(userService.createUser(any(UserRequest.class)))
            .thenReturn(null);

    mockMvc.perform(
            post("/api/users")
                    .with(user("admin").roles("MERCHANT_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
    )
            .andExpect(status().isOk());

    verify(userService).createUser(any(UserRequest.class));
}

@Test
void merchantUserShouldNotCreateUser() throws Exception {

    mockMvc.perform(
            post("/api/users")
                    .with(user("merchant").roles("MERCHANT_USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
    )
            .andExpect(status().isForbidden());

    verifyNoInteractions(userService);
}

@Test
void adminShouldRetrieveAllUsers() throws Exception {

    when(userService.getAllUsers())
            .thenReturn(List.of());

    mockMvc.perform(
            get("/api/users")
                    .with(user("admin").roles("MERCHANT_ADMIN"))
                    .accept(MediaType.APPLICATION_JSON)
    )
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

    verify(userService).getAllUsers();
}

@Test
void merchantUserShouldNotRetrieveAllUsers() throws Exception {

    mockMvc.perform(
            get("/api/users")
                    .with(user("merchant").roles("MERCHANT_USER"))
    )
            .andExpect(status().isForbidden());

    verifyNoInteractions(userService);
}

@Test
void adminShouldRetrieveUserByEmail() throws Exception {

    when(userService.getUserByEmail("merchant@example.com"))
            .thenReturn(null);

    mockMvc.perform(
            get("/api/users/email/merchant@example.com")
                    .with(user("admin").roles("MERCHANT_ADMIN"))
    )
            .andExpect(status().isNotFound());

    verify(userService)
            .getUserByEmail("merchant@example.com");
}

@Test
void adminShouldReceiveNotFoundForUnknownEmail() throws Exception {

    when(userService.getUserByEmail("unknown@example.com"))
            .thenReturn(null);

    mockMvc.perform(
            get("/api/users/email/unknown@example.com")
                    .with(user("admin").roles("MERCHANT_ADMIN"))
    )
            .andExpect(status().isNotFound());

    verify(userService)
            .getUserByEmail("unknown@example.com");
}

@Test
void unauthenticatedUserShouldNotAccessUserEndpoints()
        throws Exception {

    mockMvc.perform(
            get("/api/users")
    )
            .andExpect(status().isUnauthorized());

    verifyNoInteractions(userService);
}


}
