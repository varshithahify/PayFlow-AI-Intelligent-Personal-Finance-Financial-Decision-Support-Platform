package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.UserRequest;
import com.payflow.payflow_backend.dto.UserResponse;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.repository.UserRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final Long ORG_ID = 10L;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(ORG_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateUserSuccessfully() {

        UserRequest request = new UserRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(null);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals(ORG_ID, savedUser.getOrgId());
        assertEquals("hashedPassword", savedUser.getPasswordHash());

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldRejectDuplicateEmail() {

        UserRequest request = new UserRequest();
        request.setName("Test User");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        User existingUser = mock(User.class);

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(existingUser);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldReturnOnlyUsersFromCurrentOrganization() {

        User user = new User(
                "Test User",
                "test@example.com",
                "hashedPassword",
                ORG_ID
        );

        when(userRepository.findAllByOrgId(ORG_ID))
                .thenReturn(List.of(user));

        List<UserResponse> responses =
                userService.getAllUsers();

        assertEquals(1, responses.size());
        assertEquals("test@example.com", responses.get(0).getEmail());

        verify(userRepository).findAllByOrgId(ORG_ID);
        verify(userRepository, never()).findAll();
    }

    @Test
    void shouldReturnUserWhenEmailBelongsToCurrentOrganization() {

        User user = new User(
                "Test User",
                "test@example.com",
                "hashedPassword",
                ORG_ID
        );

        when(userRepository.findByEmailAndOrgId(
                "test@example.com", ORG_ID))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserByEmail("test@example.com");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());

        verify(userRepository)
                .findByEmailAndOrgId("test@example.com", ORG_ID);
    }

    @Test
    void shouldReturnNullWhenUserDoesNotBelongToCurrentOrganization() {

        when(userRepository.findByEmailAndOrgId(
                "other@example.com", ORG_ID))
                .thenReturn(Optional.empty());

        UserResponse response =
                userService.getUserByEmail("other@example.com");

        assertNull(response);
    }

    @Test
    void shouldRejectUserCreationWithoutOrganizationContext() {

        TenantContext.clear();

        UserRequest request = new UserRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        assertThrows(
                IllegalStateException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectUserListingWithoutOrganizationContext() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> userService.getAllUsers()
        );

        verify(userRepository, never()).findAllByOrgId(anyLong());
    }
}