package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.UserRequest;
import com.payflow.payflow_backend.dto.UserResponse;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.repository.UserRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserRequest request) {

        Long orgId = TenantContext.requireOrgId();

        // Email is globally unique in the current application.
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getName(),
                request.getEmail(),
                hashedPassword,
                orgId
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {

        Long orgId = TenantContext.requireOrgId();

        return userRepository.findAllByOrgId(orgId)
                .stream()
                .map(UserResponse::new)
                .toList();
    }

    public UserResponse getUserByEmail(String email) {

        Long orgId = TenantContext.requireOrgId();

        return userRepository.findByEmailAndOrgId(email, orgId)
                .map(UserResponse::new)
                .orElse(null);
    }
}