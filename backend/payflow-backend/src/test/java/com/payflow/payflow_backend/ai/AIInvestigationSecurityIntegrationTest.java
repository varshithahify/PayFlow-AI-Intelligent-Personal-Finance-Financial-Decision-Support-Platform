package com.payflow.payflow_backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.ai.dto.AIInvestigationFindingResponse;
import com.payflow.payflow_backend.controller.AIInvestigationController;
import com.payflow.payflow_backend.repository.AIInvestigationFindingRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.security.JwtService;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AIInvestigationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    /*
     * Keep the real AIInvestigationFindingService and JWT filter.
     * Mock only external AI investigation execution and repositories.
     */
    @MockitoBean
    private AIInvestigationService aiInvestigationService;

    @MockitoBean
    private InvestigationRepository investigationRepository;

    @MockitoBean
    private AIInvestigationFindingRepository findingRepository;

    @Autowired
    private AIInvestigationFindingService findingService;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError());

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
        ).andExpect(status().is4xxClientError());

        verifyNoInteractions(
                investigationRepository,
                findingRepository
        );
    }

    @Test
    void shouldReturnNotFoundWhenInvestigationBelongsToAnotherOrganization()
            throws Exception {

        // Organization 1 is authenticated.
        String organizationOneToken = jwtService.generateToken(
                "org-one@example.com",
                1L,
                "MERCHANT"
        );

        // Investigation 14 belongs to Organization 2.
        // A tenant-scoped lookup for Organization 1 must not find it.
        when(investigationRepository.findByIdAndOrgId(14L, 1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .header(
                                "Authorization",
                                "Bearer " + organizationOneToken
                        )
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());

        // The real service must check ownership using Organization 1.
        verify(investigationRepository)
                .findByIdAndOrgId(14L, 1L);

        // It must never retrieve findings after the ownership check fails.
        verifyNoInteractions(findingRepository);

        // An unscoped investigation lookup must not be used.
        verify(investigationRepository, never())
                .findById(14L);
    }

    @Test
    void shouldReturnFindingsForAuthenticatedOrganization()
            throws Exception {

        String organizationOneToken = jwtService.generateToken(
                "org-one@example.com",
                1L,
                "MERCHANT"
        );

        com.payflow.payflow_backend.entity.Investigation investigation =
                new com.payflow.payflow_backend.entity.Investigation();

        investigation.setId(14L);
        investigation.setOrgId(1L);
        investigation.setTransactionId(20L);

        when(investigationRepository.findByIdAndOrgId(14L, 1L))
                .thenReturn(Optional.of(investigation));

        // No findings are needed to prove that the authorized lookup works.
        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(1L, 14L))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/ai/investigations/14/findings")
                        .header(
                                "Authorization",
                                "Bearer " + organizationOneToken
                        )
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        verify(investigationRepository)
                .findByIdAndOrgId(14L, 1L);

        verify(findingRepository)
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(1L, 14L);

        verify(investigationRepository, never())
                .findById(14L);
    }
}
