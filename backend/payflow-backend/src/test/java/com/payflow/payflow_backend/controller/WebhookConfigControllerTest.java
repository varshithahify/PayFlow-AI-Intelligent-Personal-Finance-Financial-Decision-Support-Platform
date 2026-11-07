package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.WebhookConfigRequest;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.repository.UserRepository;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.service.WebhookConfigService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebhookConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookConfigService webhookConfigService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "merchant@example.com",
                "password"
        );
    }


    private User createUser() {
        return new User(
                "Merchant",
                "merchant@example.com",
                "encodedPassword",
                10L
        );
    }


    private WebhookConfig createWebhook(Long id) {

        WebhookConfig webhook = new WebhookConfig(
                10L,
                "https://example.com/webhook",
                "secret-key",
                "payment.success,payment.failed",
                true
        );

        webhook.setId(id);

        return webhook;
    }


    private String validRequest() {
        return """
                {
                    "endpointUrl": "https://example.com/webhook",
                    "secretKey": "secret-key",
                    "events": "payment.success,payment.failed"
                }
                """;
    }


    // --------------------------------------------------
    // CREATE WEBHOOK
    // --------------------------------------------------

    @Test
    void shouldCreateWebhookSuccessfully() throws Exception {

        WebhookConfig webhook = createWebhook(1L);

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        when(webhookConfigService.createWebhook(
                10L,
                "https://example.com/webhook",
                "secret-key",
                "payment.success,payment.failed"
        )).thenReturn(webhook);

        mockMvc.perform(
                post("/api/webhooks")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.orgId").value(10))
        .andExpect(jsonPath("$.endpointUrl")
                .value("https://example.com/webhook"))
        .andExpect(jsonPath("$.active").value(true));

        verify(userRepository)
                .findByEmail("merchant@example.com");

        verify(webhookConfigService).createWebhook(
                10L,
                "https://example.com/webhook",
                "secret-key",
                "payment.success,payment.failed"
        );
    }


    @Test
    void shouldRejectWebhookCreationWhenEndpointUrlIsMissing()
            throws Exception {

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        mockMvc.perform(
                post("/api/webhooks")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "secretKey": "secret-key",
                                    "events": "payment.success"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookConfigService);
    }


    // --------------------------------------------------
    // GET ALL WEBHOOKS
    // --------------------------------------------------

    @Test
    void shouldReturnAllWebhooksForAuthenticatedOrganization()
            throws Exception {

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        when(webhookConfigService.getWebhooks(10L))
                .thenReturn(List.of(
                        createWebhook(1L),
                        createWebhook(2L)
                ));

        mockMvc.perform(
                get("/api/webhooks")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].orgId").value(10))
        .andExpect(jsonPath("$[1].orgId").value(10));

        verify(webhookConfigService).getWebhooks(10L);
    }


    @Test
    void shouldReturnEmptyListWhenNoWebhooksExist()
            throws Exception {

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        when(webhookConfigService.getWebhooks(10L))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/webhooks")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(webhookConfigService).getWebhooks(10L);
    }


    // --------------------------------------------------
    // GET WEBHOOK BY ID
    // --------------------------------------------------

    @Test
    void shouldReturnWebhookById() throws Exception {

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        when(webhookConfigService.getWebhook(10L, 1L))
                .thenReturn(createWebhook(1L));

        mockMvc.perform(
                get("/api/webhooks/1")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.orgId").value(10));

        verify(webhookConfigService).getWebhook(10L, 1L);
    }


    // --------------------------------------------------
    // UPDATE WEBHOOK
    // --------------------------------------------------

    @Test
    void shouldUpdateWebhookSuccessfully() throws Exception {

        WebhookConfig webhook = createWebhook(1L);
        webhook.setEndpointUrl("https://example.com/updated");

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        when(webhookConfigService.updateWebhook(
                10L,
                1L,
                "https://example.com/webhook",
                "secret-key",
                "payment.success,payment.failed",
                true
        )).thenReturn(webhook);

        mockMvc.perform(
                put("/api/webhooks/1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.endpointUrl")
                .value("https://example.com/updated"));

        verify(webhookConfigService).updateWebhook(
                10L,
                1L,
                "https://example.com/webhook",
                "secret-key",
                "payment.success,payment.failed",
                true
        );
    }


    // --------------------------------------------------
    // DELETE WEBHOOK
    // --------------------------------------------------

    @Test
    void shouldDeleteWebhookSuccessfully() throws Exception {

        when(userRepository.findByEmail("merchant@example.com"))
                .thenReturn(createUser());

        doNothing().when(webhookConfigService)
                .deleteWebhook(10L, 1L);

        mockMvc.perform(
                delete("/api/webhooks/1")
                        .principal(authentication())
        )
        .andExpect(status().isNoContent());

        verify(webhookConfigService)
                .deleteWebhook(10L, 1L);
    }
}