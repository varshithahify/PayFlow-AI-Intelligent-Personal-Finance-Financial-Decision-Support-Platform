package com.payflow.payflow_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.core.Authentication;
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
    void shouldAuthenticateValidAdminToken()
            throws ServletException, IOException {

        String token = jwtService.generateToken(
                "admin@example.com",
                1L,
                "MERCHANT_ADMIN"
        );

        MockHttpServletRequest request =
                createRequest("Bearer " + token);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {

            Authentication authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            assertNotNull(authentication);

            assertEquals(
                    "admin@example.com",
                    authentication.getName()
            );

            assertTrue(
                    authentication.getAuthorities()
                            .stream()
                            .anyMatch(authority ->
                                    authority.getAuthority()
                                            .equals("ROLE_MERCHANT_ADMIN")
                            )
            );

            assertEquals(1L, TenantContext.requireOrgId());

            return null;

        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldAuthenticateValidMerchantUserToken()
            throws ServletException, IOException {

        String token = jwtService.generateToken(
                "merchant@example.com",
                2L,
                "MERCHANT_USER"
        );

        MockHttpServletRequest request =
                createRequest("Bearer " + token);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {

            Authentication authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            assertNotNull(authentication);

            assertTrue(
                    authentication.getAuthorities()
                            .stream()
                            .anyMatch(authority ->
                                    authority.getAuthority()
                                            .equals("ROLE_MERCHANT_USER")
                            )
            );

            assertEquals(2L, TenantContext.requireOrgId());

            return null;

        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldNotAuthenticateInvalidToken()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                createRequest("Bearer invalid.jwt.token");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
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
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenBearerTokenIsEmpty()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                createRequest("Bearer ");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldIgnoreAuthorizationHeaderWithIncorrectPrefix()
            throws ServletException, IOException {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT_USER"
        );

        MockHttpServletRequest request =
                createRequest("Basic " + token);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldClearTenantContextWhenFilterChainThrowsException()
            throws ServletException, IOException {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT_ADMIN"
        );

        MockHttpServletRequest request =
                createRequest("Bearer " + token);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {

            assertEquals(1L, TenantContext.requireOrgId());

            throw new ServletException("Simulated downstream failure");

        }).when(filterChain).doFilter(request, response);

        assertThrows(
                ServletException.class,
                () -> filter.doFilter(request, response, filterChain)
        );

        assertNull(
                TenantContext.getOrgId(),
                "Tenant context must be cleared even after an exception"
        );
    }

    private MockHttpServletRequest createRequest(String authorization) {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader("Authorization", authorization);

        return request;
    }
}
