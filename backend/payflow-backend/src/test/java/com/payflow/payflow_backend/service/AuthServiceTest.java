package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.LoginRequest;
import com.payflow.payflow_backend.dto.RegisterRequest;
import com.payflow.payflow_backend.entity.Role;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.repository.UserRepository;
import com.payflow.payflow_backend.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequest();
        registerRequest.setName("Varshitha");
        registerRequest.setEmail("varshitha@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("varshitha@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(null);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(registerRequest);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("Varshitha", result.getName()),
                () -> assertEquals(
                        "varshitha@example.com",
                        result.getEmail()
                ),
                () -> assertEquals(
                        "encoded-password",
                        result.getPasswordHash()
                ),
                () -> assertEquals(1L, result.getOrgId()),
                () -> assertEquals(Role.MERCHANT_ADMIN, result.getRole()),
                () -> assertNotNull(result.getCreatedAt())
        );

        verify(userRepository).findByEmail("varshitha@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        User existingUser = new User(
                "Existing User",
                "varshitha@example.com",
                "existing-hash"
        );

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(existingUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfullyAndGenerateJwt() {

        User user = new User(
                "Varshitha",
                "varshitha@example.com",
                "encoded-password",
                1L,
                Role.MERCHANT_ADMIN
        );

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(user);

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateToken(
                "varshitha@example.com",
                1L,
                "MERCHANT_ADMIN"
        )).thenReturn("mock-jwt-token");

        String token = authService.login(loginRequest);

        assertEquals("mock-jwt-token", token);

        verify(userRepository).findByEmail("varshitha@example.com");

        verify(passwordEncoder).matches(
                "password123",
                "encoded-password"
        );

        verify(jwtService).generateToken(
                "varshitha@example.com",
                1L,
                "MERCHANT_ADMIN"
        );
    }

    @Test
    void shouldRejectLoginWhenUserDoesNotExist() {

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder, never()).matches(any(), any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRejectLoginWhenPasswordIsIncorrect() {

        User user = new User(
                "Varshitha",
                "varshitha@example.com",
                "encoded-password"
        );

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(user);

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder).matches(
                "password123",
                "encoded-password"
        );

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldSaveUserWithEncodedPasswordAndDefaultOrganization() {

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(null);

        when(passwordEncoder.encode("password123"))
                .thenReturn("secure-hash");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals("Varshitha", savedUser.getName()),
                () -> assertEquals(
                        "varshitha@example.com",
                        savedUser.getEmail()
                ),
                () -> assertEquals(
                        "secure-hash",
                        savedUser.getPasswordHash()
                ),
                () -> assertEquals(1L, savedUser.getOrgId()),
                () -> assertEquals(
                        Role.MERCHANT_ADMIN,
                        savedUser.getRole()
                )
        );
    }

    @Test
    void shouldPropagateRepositoryFailureDuringRegistration() {

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(null);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals(
                "Database unavailable",
                exception.getMessage()
        );

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldGenerateJwtUsingActualOrganizationAndRole() {

        User user = new User(
                "Merchant User",
                "varshitha@example.com",
                "encoded-password",
                5L,
                Role.MERCHANT_USER
        );

        when(userRepository.findByEmail("varshitha@example.com"))
                .thenReturn(user);

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateToken(
                "varshitha@example.com",
                5L,
                "MERCHANT_USER"
        )).thenReturn("merchant-user-token");

        String token = authService.login(loginRequest);

        assertEquals("merchant-user-token", token);

        verify(jwtService).generateToken(
                "varshitha@example.com",
                5L,
                "MERCHANT_USER"
        );
    }
}
