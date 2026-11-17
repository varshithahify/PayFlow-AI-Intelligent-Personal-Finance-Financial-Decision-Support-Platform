package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.CreateSettlementJobRequest;
import com.payflow.payflow_backend.entity.SettlementJob;
import com.payflow.payflow_backend.entity.SettlementJobExecution;
import com.payflow.payflow_backend.entity.SettlementJobStatus;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.security.TenantContext;
import com.payflow.payflow_backend.service.SettlementJobManagementService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettlementJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class SettlementJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementJobManagementService settlementJobService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private SettlementJob createJob(String jobId) {
        SettlementJob job = new SettlementJob();
        job.setId(1L);
        job.setJobId(jobId);
        job.setOrgId(1L);
        job.setStatus(SettlementJobStatus.PENDING);
        job.setTotalTransactions(2);
        job.setProcessedCount(0);
        job.setDiscrepancyCount(0);
        job.setCreatedAt(LocalDateTime.now());
        return job;
    }

    private SettlementJobExecution createExecution() {
        SettlementJobExecution execution = new SettlementJobExecution();
        execution.setId(10L);
        execution.setSettlementJobId(1L);
        execution.setAttemptNumber(1);
        execution.setProcessedCount(2);
        execution.setDiscrepancyCount(0);
        return execution;
    }

    @Test
    void createSettlementJobReturnsCreated() throws Exception {
        SettlementJob job = createJob("job-123");

        when(settlementJobService.createSettlementJob(any(CreateSettlementJobRequest.class)))
                .thenReturn(job);

        mockMvc.perform(post("/api/settlement-jobs")
                        .contentType("application/json")
                        .content("{\"transactionIds\":[10,20]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(settlementJobService).createSettlementJob(any(CreateSettlementJobRequest.class));
    }

    @Test
    void createSettlementJobRejectsEmptyTransactionList() throws Exception {
        mockMvc.perform(post("/api/settlement-jobs")
                        .contentType("application/json")
                        .content("{\"transactionIds\":[]}"))
                .andExpect(status().isBadRequest());

        verify(settlementJobService, never())
                .createSettlementJob(any(CreateSettlementJobRequest.class));
    }

    @Test
    void getSettlementJobsReturnsPage() throws Exception {
        SettlementJob job = createJob("job-123");
        Page<SettlementJob> page = new PageImpl<>(List.of(job));

        when(settlementJobService.getSettlementJobs(
                isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/settlement-jobs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobId").value("job-123"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(settlementJobService)
                .getSettlementJobs(isNull(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void getSettlementJobsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/settlement-jobs")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(settlementJobService, never())
                .getSettlementJobs(any(), any());
    }

    @Test
    void getSettlementJobsSupportsStatusFilter() throws Exception {
        SettlementJob job = createJob("job-123");
        Page<SettlementJob> page = new PageImpl<>(List.of(job));

        when(settlementJobService.getSettlementJobs(
                eq(SettlementJobStatus.PENDING),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/settlement-jobs")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(settlementJobService).getSettlementJobs(
                eq(SettlementJobStatus.PENDING),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void getSettlementJobReturnsDetails() throws Exception {
        SettlementJob job = createJob("job-123");

        when(settlementJobService.getSettlementJob("job-123"))
                .thenReturn(job);

        mockMvc.perform(get("/api/settlement-jobs/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.orgId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(settlementJobService).getSettlementJob("job-123");
    }

    @Test
    void executeSettlementJobReturnsUpdatedJob() throws Exception {
        SettlementJob job = createJob("job-123");
        job.setStatus(SettlementJobStatus.COMPLETED);
        job.setProcessedCount(2);

        when(settlementJobService.executeSettlementJob("job-123"))
                .thenReturn(job);

        mockMvc.perform(post("/api/settlement-jobs/job-123/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processedCount").value(2));

        verify(settlementJobService).executeSettlementJob("job-123");
    }

    @Test
    void getExecutionHistoryReturnsList() throws Exception {
        when(settlementJobService.getExecutionHistory("job-123"))
                .thenReturn(List.of(createExecution()));

        mockMvc.perform(get("/api/settlement-jobs/job-123/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].attemptNumber").value(1));

        verify(settlementJobService).getExecutionHistory("job-123");
    }

    @Test
    void getExecutionHistoryReturnsEmptyList() throws Exception {
        when(settlementJobService.getExecutionHistory("job-123"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/settlement-jobs/job-123/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}