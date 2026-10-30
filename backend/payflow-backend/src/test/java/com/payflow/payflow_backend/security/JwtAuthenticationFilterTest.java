package com.payflow.payflow_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "PayFlowAITestSecretKeyMustBeAtLeast32BytesLong!",
                3_600_000L
        );

        filter = new JwtAuthenticationFilter(jwtService);

        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateValidTokenAndClearTenantContext()
            throws ServletException, IOException {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT"
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(
                "merchant@example.com",
                SecurityContextHolder.getContext()
                        .getAuthentication().getName()
        );

        assertNull(
                TenantContext.getOrgId(),
                "Tenant context should be cleared after request processing"
        );
    }

    @Test
    void shouldNotAuthenticateInvalidToken()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer invalid.jwt.token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsMissing()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertNull(TenantContext.getOrgId());
    }
}
