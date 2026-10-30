package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.ai.AIInvestigationFindingService;
import com.payflow.payflow_backend.ai.AIInvestigationService;
import com.payflow.payflow_backend.ai.dto.AIInvestigationFindingResponse;
import com.payflow.payflow_backend.ai.dto.AIInvestigationResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AIInvestigationControllerTest {

    @Mock
    private AIInvestigationFindingService findingService;

    @Mock
    private AIInvestigationService aiInvestigationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AIInvestigationController controller =
                new AIInvestigationController(
                        aiInvestigationService,
                        findingService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    // Test 1: GET should return saved AI findings.
    @Test
    void shouldReturnFindingsForInvestigation() throws Exception {

        AIInvestigationFindingResponse response =
                new AIInvestigationFindingResponse(
                        1L,
                        14L,
                        20L,
                        "No matching external payment record found",
                        List.of("External record missing"),
                        List.of("Verify external record"),
                        List.of("External system unavailable"),
                        true,
                        "llama3.2:3b",
                        LocalDateTime.now());

        when(findingService.getFindings(14L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/ai/investigations/14/findings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].investigationId").value(14))
                .andExpect(jsonPath("$[0].transactionId").value(20))
                .andExpect(jsonPath("$[0].likelyCause")
                        .value("No matching external payment record found"))
                .andExpect(jsonPath("$[0].requiresHumanReview").value(true));

        verify(findingService).getFindings(14L);
    }

    // Test 2: An unavailable investigation should return 404.
    @Test
    void shouldReturnNotFoundWhenInvestigationIsUnavailable()
            throws Exception {

        when(findingService.getFindings(15L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Investigation not found"));

        mockMvc.perform(
                        get("/api/ai/investigations/15/findings"))
                .andExpect(status().isNotFound());

        verify(findingService).getFindings(15L);
    }

    // Test 3: No findings should return an empty JSON array.
    @Test
    void shouldReturnEmptyArrayWhenNoFindingsExist()
            throws Exception {

        when(findingService.getFindings(14L))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/ai/investigations/14/findings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(findingService).getFindings(14L);
    }

    // Test 4: POST should return the AI investigation result.
    @Test
    void shouldReturnAIInvestigationResult() throws Exception {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "No matching external payment record found",
                        List.of("External payment record is missing"),
                        List.of("Verify the external payment record"),
                        true,
                        List.of("External payment data is unavailable"));

        when(aiInvestigationService.investigate(20L))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/ai/investigations/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likelyCause")
                        .value("No matching external payment record found"))
                .andExpect(jsonPath("$.requiresHumanReview").value(true))
                .andExpect(jsonPath("$.evidence[0]")
                        .value("External payment record is missing"))
                .andExpect(jsonPath("$.recommendedActions[0]")
                        .value("Verify the external payment record"))
                .andExpect(jsonPath("$.limitations[0]")
                        .value("External payment data is unavailable"));

        verify(aiInvestigationService).investigate(20L);
    }
}

