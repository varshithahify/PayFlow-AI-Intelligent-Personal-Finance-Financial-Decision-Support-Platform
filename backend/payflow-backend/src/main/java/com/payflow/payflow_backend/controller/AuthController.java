package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.LoginRequest;
import com.payflow.payflow_backend.dto.LoginResponse;
import com.payflow.payflow_backend.dto.RegisterRequest;
import com.payflow.payflow_backend.dto.UserResponse;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request);

        UserResponse response = new UserResponse(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        String token = authService.login(request);

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }
}