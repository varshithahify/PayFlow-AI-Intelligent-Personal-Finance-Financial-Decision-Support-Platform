package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.WebhookConfigRequest;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.repository.UserRepository;
import com.payflow.payflow_backend.service.WebhookConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookConfigController {

    private final WebhookConfigService webhookConfigService;
    private final UserRepository userRepository;

    public WebhookConfigController(
            WebhookConfigService webhookConfigService,
            UserRepository userRepository) {
        this.webhookConfigService = webhookConfigService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<WebhookConfig> createWebhook(
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                webhookConfigService.createWebhook(
                        user.getOrgId(),
                        request.getEndpointUrl(),
                        request.getSecretKey(),
                        request.getEvents()));
    }

    @GetMapping
    public ResponseEntity<List<WebhookConfig>> getWebhooks(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                webhookConfigService.getWebhooks(user.getOrgId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfig> getWebhook(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                webhookConfigService.getWebhook(
                        user.getOrgId(),
                        id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfig> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                webhookConfigService.updateWebhook(
                        user.getOrgId(),
                        id,
                        request.getEndpointUrl(),
                        request.getSecretKey(),
                        request.getEvents(),
                        true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        webhookConfigService.deleteWebhook(
                user.getOrgId(),
                id);

        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName());

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        return user;
    }
}