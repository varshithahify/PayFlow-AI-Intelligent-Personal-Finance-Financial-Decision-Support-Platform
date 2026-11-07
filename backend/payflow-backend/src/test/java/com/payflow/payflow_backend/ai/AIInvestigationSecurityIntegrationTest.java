package com.payflow.payflow_backend.ai;

import com.payflow.payflow_backend.dto.LoginRequest;
import com.payflow.payflow_backend.dto.RegisterRequest;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.repository.AIInvestigationFindingRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.JwtService;
import com.payflow.payflow_backend.security.TenantContext;
import com.payflow.payflow_backend.service.AuthService;
import com.payflow.payflow_backend.service.ExternalPaymentSimulatorService;
import com.payflow.payflow_backend.service.UserService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AIInvestigationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockitoBean
    private AIInvestigationService aiInvestigationService;

    @MockitoBean
    private InvestigationRepository investigationRepository;

    @MockitoBean
    private AIInvestigationFindingRepository findingRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private ExternalPaymentSimulatorService simulatorService;

    @MockitoBean
    private AuthService authService;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    private String generateToken(Long orgId, String role) {
        return jwtService.generateToken(
                "test@example.com",
                orgId,
                role
        );
    }

    // =========================================================
    // JWT AUTHENTICATION AND INVESTIGATION SECURITY
    // =========================================================

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isUnauthorized());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(
                investigationRepository,
                findingRepository
        );
    }

    @Test
    void shouldRejectRequestWithInvalidJwt() throws Exception {

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .header("Authorization", "Bearer invalid-token")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isUnauthorized());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(
                investigationRepository,
                findingRepository
        );
    }

    @Test
    void shouldRejectRequestWithExpiredJwt() throws Exception {

        SecretKey secretKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        Date now = new Date();

        String expiredToken = Jwts.builder()
                .subject("org-one@example.com")
                .claim("orgId", 1L)
                .claim("role", "MERCHANT_ADMIN")
                .issuedAt(new Date(now.getTime() - 120_000))
                .expiration(new Date(now.getTime() - 60_000))
                .signWith(secretKey)
                .compact();

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isUnauthorized());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(
                investigationRepository,
                findingRepository
        );
    }

    @Test
    void shouldReturnNotFoundWhenInvestigationBelongsToAnotherOrganization()
            throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        when(investigationRepository.findByIdAndOrgId(14L, 1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());

        assertNull(TenantContext.getOrgId());

        verify(investigationRepository)
                .findByIdAndOrgId(14L, 1L);

        verifyNoInteractions(findingRepository);

        verify(investigationRepository, never())
                .findById(14L);
    }

    @Test
    void shouldReturnFindingsForAuthenticatedOrganization()
            throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        Investigation investigation = new Investigation();

        investigation.setId(14L);
        investigation.setOrgId(1L);
        investigation.setTransactionId(20L);

        when(investigationRepository.findByIdAndOrgId(14L, 1L))
                .thenReturn(Optional.of(investigation));

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(1L, 14L))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        assertNull(TenantContext.getOrgId());

        verify(investigationRepository)
                .findByIdAndOrgId(14L, 1L);

        verify(findingRepository)
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(1L, 14L);

        verify(investigationRepository, never())
                .findById(14L);
    }

    // =========================================================
    // USER CONTROLLER ROLE AUTHORIZATION
    // =========================================================

    @Test
    void shouldAllowAdminToGetAllUsers() throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        when(userService.getAllUsers())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        assertNull(TenantContext.getOrgId());

        verify(userService).getAllUsers();
    }

    @Test
    void shouldRejectMerchantUserWhenGettingAllUsers() throws Exception {

        String token = generateToken(1L, "MERCHANT_USER");

        mockMvc.perform(
                get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isForbidden());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldAllowAdminToCreateUser() throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        mockMvc.perform(
                post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isOk());

        assertNull(TenantContext.getOrgId());

        verify(userService).createUser(any());
    }

    @Test
    void shouldRejectMerchantUserWhenCreatingUser() throws Exception {

        String token = generateToken(1L, "MERCHANT_USER");

        mockMvc.perform(
                post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isForbidden());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldAllowAdminToGetUserByEmail() throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        when(userService.getUserByEmail("test@example.com"))
                .thenReturn(null);

        mockMvc.perform(
                get("/api/users/email/test@example.com")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());

        assertNull(TenantContext.getOrgId());

        verify(userService)
                .getUserByEmail("test@example.com");
    }

    @Test
    void shouldRejectMerchantUserWhenGettingUserByEmail()
            throws Exception {

        String token = generateToken(1L, "MERCHANT_USER");

        mockMvc.perform(
                get("/api/users/email/test@example.com")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isForbidden());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(userService);
    }

    // =========================================================
    // EXTERNAL PAYMENT SIMULATOR AUTHORIZATION
    // =========================================================

    @Test
    void shouldAllowAdminToAccessExternalPaymentSimulator()
            throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        when(transactionRepository.findByIdAndOrgId(20L, 1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                post("/api/simulator/external-payment/20")
                        .param("gateway", "STRIPE")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());

        assertNull(TenantContext.getOrgId());

        verify(transactionRepository)
                .findByIdAndOrgId(20L, 1L);

        verifyNoInteractions(simulatorService);
    }

    @Test
    void shouldRejectMerchantUserFromExternalPaymentSimulator()
            throws Exception {

        String token = generateToken(1L, "MERCHANT_USER");

        mockMvc.perform(
                post("/api/simulator/external-payment/20")
                        .param("gateway", "STRIPE")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isForbidden());

        assertNull(TenantContext.getOrgId());

        verifyNoInteractions(
                transactionRepository,
                simulatorService
        );
    }

    // =========================================================
    // PUBLIC AUTHENTICATION ENDPOINTS
    // =========================================================

    @Test
    void shouldAllowPublicRegistrationWithoutJwt() throws Exception {

        User user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(user);

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "test@example.com",
                                  "password": "password123"
                                }
                                """)
        ).andExpect(status().isCreated());

        verify(authService)
                .register(any(RegisterRequest.class));
    }

    @Test
    void shouldAllowPublicLoginWithoutJwt() throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenReturn("mock-jwt-token");

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "password123"
                                }
                                """)
        ).andExpect(status().isOk());

        verify(authService)
                .login(any(LoginRequest.class));
    }

    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Test
    void shouldAllowConfiguredFrontendOrigin() throws Exception {

        mockMvc.perform(
                options("/api/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
        )
        .andExpect(status().isOk())
        .andExpect(header().string(
                "Access-Control-Allow-Origin",
                "http://localhost:5173"
        ));
    }

    @Test
    void shouldRejectUnconfiguredFrontendOrigin() throws Exception {

        MvcResult result = mockMvc.perform(
                options("/api/users")
                        .header("Origin", "http://malicious.example")
                        .header("Access-Control-Request-Method", "GET")
        ).andReturn();

        assertNull(
                result.getResponse()
                        .getHeader("Access-Control-Allow-Origin")
        );
    }

    @Test
    void shouldAllowAuthorizationHeaderInCorsPreflight() throws Exception {

        mockMvc.perform(
                options("/api/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header(
                                "Access-Control-Request-Headers",
                                "authorization,content-type"
                        )
        )
        .andExpect(status().isOk())
        .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    // =========================================================
    // STATELESS SESSION CONFIGURATION
    // =========================================================

    @Test
    void shouldNotCreateHttpSessionForAuthenticatedRequest()
            throws Exception {

        String token = generateToken(1L, "MERCHANT_ADMIN");

        when(userService.getAllUsers())
                .thenReturn(List.of());

        MvcResult result = mockMvc.perform(
                get("/api/users")
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk()).andReturn();

        assertNull(result.getRequest().getSession(false));

        assertNull(
                result.getResponse().getHeader("Set-Cookie")
        );

        assertNull(TenantContext.getOrgId());
    }

    @Test
    void shouldKeepRegistrationAccessibleWithInvalidAuthenticationHeader()
            throws Exception {

        User user = new User(
                "Test User",
                "public@example.com",
                "encoded-password"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(user);

        mockMvc.perform(
                post("/auth/register")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "public@example.com",
                                  "password": "password123"
                                }
                                """)
        ).andExpect(status().isCreated());

        verify(authService)
                .register(any(RegisterRequest.class));
    }
}
