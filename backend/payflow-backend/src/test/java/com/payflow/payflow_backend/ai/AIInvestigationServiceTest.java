package com.payflow.payflow_backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.ai.dto.AIInvestigationContext;
import com.payflow.payflow_backend.ai.dto.AIInvestigationResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AIInvestigationServiceTest {

    @Mock
    private AIInvestigationContextService contextService;

    @Mock
    private AIInvestigationFindingService findingService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ObjectMapper objectMapper;
    private AIInvestigationService service;
    private AutoCloseable mocks;

    private static final Long TRANSACTION_ID = 100L;
    private static final Long INVESTIGATION_ID = 200L;

    @BeforeEach
    void setUp() {

        mocks = MockitoAnnotations.openMocks(this);

objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new AIInvestigationService(
                contextService,
                findingService,
                objectMapper,
                httpClient
        );

        when(contextService.buildContext(TRANSACTION_ID))
                .thenReturn(createContext(INVESTIGATION_ID));
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private AIInvestigationContext createContext(Long investigationId) {

        return new AIInvestigationContext(
                TRANSACTION_ID,
                new java.math.BigDecimal("1500.00"),
                "INR",
                "UPI",
                "SUCCESS",
                "RAZORPAY",
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                investigationId,
                "AMOUNT_MISMATCH",
                "HIGH",
                "OPEN",
                "Payment amount mismatch"
        );
    }

    private String validAIResponse() {

        return """
                {
                    "likelyCause": "Possible gateway inconsistency",
                    "evidence": [
                        "Internal payment amount differs from external record"
                    ],
                    "recommendedActions": [
                        "Verify gateway transaction records"
                    ],
                    "requiresHumanReview": true,
                    "limitations": [
                        "External gateway data may be delayed"
                    ]
                }
                """;
    }

    private void mockSuccessfulOllamaResponse(String generatedContent)
        throws Exception {

    // Create the root Ollama response.
    var root = objectMapper.createObjectNode();

    // Add the nested message.content field.
    root.putObject("message")
            .put("content", generatedContent);

    String ollamaBody = root.toString();

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(ollamaBody);

    when(httpClient.send(
            any(),
            any(HttpResponse.BodyHandler.class)
    )).thenReturn(httpResponse);
}

    @Test
    void shouldSuccessfullyInvestigateTransaction() throws Exception {

        mockSuccessfulOllamaResponse(validAIResponse());

        AIInvestigationResponse result =
                service.investigate(TRANSACTION_ID);

        assertNotNull(result);

        assertEquals(
                "Possible gateway inconsistency",
                result.likelyCause()
        );

        assertTrue(result.requiresHumanReview());

        verify(findingService).saveFinding(
                eq(TRANSACTION_ID),
                eq(INVESTIGATION_ID),
                any(AIInvestigationResponse.class)
        );
    }

    @Test
    void shouldParseMarkdownJsonResponse() throws Exception {

        mockSuccessfulOllamaResponse(
                "```json\n" + validAIResponse() + "\n```"
        );

        AIInvestigationResponse result =
                service.investigate(TRANSACTION_ID);

        assertNotNull(result);

        assertEquals(
                "Possible gateway inconsistency",
                result.likelyCause()
        );

        verify(findingService).saveFinding(
                eq(TRANSACTION_ID),
                eq(INVESTIGATION_ID),
                any()
        );
    }

    @Test
    void shouldRejectOllamaHttpError() throws Exception {

        when(httpResponse.statusCode()).thenReturn(500);

        when(httpClient.send(
                any(),
                any(HttpResponse.BodyHandler.class)
        )).thenReturn(httpResponse);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertEquals("Ollama returned HTTP 500", exception.getMessage());

        verify(findingService, never()).saveFinding(
                anyLong(),
                anyLong(),
                any()
        );
    }

    @Test
    void shouldRejectEmptyOllamaMessage() throws Exception {

        mockSuccessfulOllamaResponse("");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertEquals(
                "Ollama returned an empty message",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectInvalidGeneratedJson() throws Exception {

        mockSuccessfulOllamaResponse("{ invalid json }");

        assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        verify(findingService, never()).saveFinding(
                anyLong(),
                anyLong(),
                any()
        );
    }

    @Test
    void shouldRejectInvestigationWithoutInvestigationId()
            throws Exception {

        when(contextService.buildContext(TRANSACTION_ID))
                .thenReturn(createContext(null));

        mockSuccessfulOllamaResponse(validAIResponse());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertEquals(
                "Cannot save AI finding because no investigation exists",
                exception.getMessage()
        );

        verify(findingService, never()).saveFinding(
                anyLong(),
                anyLong(),
                any()
        );
    }

    @Test
    void shouldRejectMissingLikelyCause() throws Exception {

        mockSuccessfulOllamaResponse("""
                {
                    "likelyCause": " ",
                    "evidence": ["Evidence"],
                    "recommendedActions": ["Review"],
                    "requiresHumanReview": true,
                    "limitations": ["Limited data"]
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {

        mockSuccessfulOllamaResponse("""
                {
                    "likelyCause": "Possible mismatch",
                    "evidence": null,
                    "recommendedActions": ["Review"],
                    "requiresHumanReview": true,
                    "limitations": ["Limited data"]
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );
    }

    @Test
    void shouldRejectHumanReviewDisabled() throws Exception {

        mockSuccessfulOllamaResponse("""
                {
                    "likelyCause": "Possible mismatch",
                    "evidence": ["Evidence"],
                    "recommendedActions": ["Review"],
                    "requiresHumanReview": false,
                    "limitations": ["Limited data"]
                }
                """);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertEquals(
                "AI investigation must require human review",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankEvidence() throws Exception {

        mockSuccessfulOllamaResponse("""
                {
                    "likelyCause": "Possible mismatch",
                    "evidence": [" "],
                    "recommendedActions": ["Review"],
                    "requiresHumanReview": true,
                    "limitations": ["Limited data"]
                }
                """);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertTrue(exception.getMessage().contains("evidence"));
    }

    @Test
    void shouldRejectBlankRecommendedAction() throws Exception {

        mockSuccessfulOllamaResponse("""
                {
                    "likelyCause": "Possible mismatch",
                    "evidence": ["Evidence"],
                    "recommendedActions": [""],
                    "requiresHumanReview": true,
                    "limitations": ["Limited data"]
                }
                """);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertTrue(exception.getMessage().contains("recommended actions"));
    }

    @Test
    void shouldRejectBlankLimitation() throws Exception {

        mockSuccessfulOllamaResponse("""
                {
                    "likelyCause": "Possible mismatch",
                    "evidence": ["Evidence"],
                    "recommendedActions": ["Review"],
                    "requiresHumanReview": true,
                    "limitations": [" "]
                }
                """);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertTrue(exception.getMessage().contains("limitations"));
    }

    @Test
    void shouldRestoreInterruptStatusWhenRequestInterrupted()
            throws Exception {

        when(httpClient.send(
                any(),
                any(HttpResponse.BodyHandler.class)
        )).thenThrow(new InterruptedException("Request interrupted"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertEquals(
                "AI investigation was interrupted",
                exception.getMessage()
        );

        assertTrue(Thread.currentThread().isInterrupted());

        Thread.interrupted();
    }

    @Test
    void shouldHandleIOExceptionDuringHttpRequest() throws Exception {

        when(httpClient.send(
                any(),
                any(HttpResponse.BodyHandler.class)
        )).thenThrow(new IOException("Connection failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.investigate(TRANSACTION_ID)
        );

        assertTrue(
                exception.getMessage().contains("Connection failed")
        );
    }
}