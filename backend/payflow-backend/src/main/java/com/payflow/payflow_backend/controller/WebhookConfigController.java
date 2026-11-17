package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.WebhookConfigRequest;
import com.payflow.payflow_backend.dto.WebhookConfigResponse;
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
    public ResponseEntity<WebhookConfigResponse> createWebhook(
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        WebhookConfig webhook = webhookConfigService.createWebhook(
                user.getOrgId(),
                request.getEndpointUrl(),
                request.getSecretKey(),
                request.getEvents()
        );

        return ResponseEntity.ok(
                WebhookConfigResponse.from(webhook)
        );
    }

    @GetMapping
    public ResponseEntity<List<WebhookConfigResponse>> getWebhooks(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        List<WebhookConfigResponse> response =
                webhookConfigService.getWebhooks(user.getOrgId())
                        .stream()
                        .map(WebhookConfigResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> getWebhook(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        WebhookConfig webhook =
                webhookConfigService.getWebhook(user.getOrgId(), id);

        return ResponseEntity.ok(
                WebhookConfigResponse.from(webhook)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        WebhookConfig webhook = webhookConfigService.updateWebhook(
                user.getOrgId(),
                id,
                request.getEndpointUrl(),
                request.getSecretKey(),
                request.getEvents(),
                true
        );

        return ResponseEntity.ok(
                WebhookConfigResponse.from(webhook)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        webhookConfigService.deleteWebhook(user.getOrgId(), id);

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