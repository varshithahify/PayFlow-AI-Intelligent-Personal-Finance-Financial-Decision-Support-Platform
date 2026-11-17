package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.CreateSettlementJobRequest;
import com.payflow.payflow_backend.entity.*;
import com.payflow.payflow_backend.event.SettlementJobEvent;
import com.payflow.payflow_backend.event.SettlementJobEventProducer;
import com.payflow.payflow_backend.event.SettlementJobEventType;
import com.payflow.payflow_backend.repository.*;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SettlementJobManagementServiceTest {

    private SettlementJobRepository jobRepository;
    private SettlementJobTransactionRepository jobTransactionRepository;
    private SettlementJobExecutionRepository executionRepository;
    private TransactionRepository transactionRepository;
    private ReconciliationService reconciliationService;
    private SettlementJobEventProducer eventProducer;
    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;

    private SettlementJobManagementService service;

    private static final Long ORG_ID = 10L;
    private static final Long TRANSACTION_ID = 100L;
    private static final String JOB_ID = "settlement-test-001";

    @BeforeEach
    void setUp() {

        TenantContext.setOrgId(ORG_ID);

        jobRepository = mock(SettlementJobRepository.class);
        jobTransactionRepository =
                mock(SettlementJobTransactionRepository.class);
        executionRepository =
                mock(SettlementJobExecutionRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        reconciliationService = mock(ReconciliationService.class);
        eventProducer = mock(SettlementJobEventProducer.class);

        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);

        when(eventProducer.publish(any(SettlementJobEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service = new SettlementJobManagementService(
                jobRepository,
                jobTransactionRepository,
                executionRepository,
                transactionRepository,
                reconciliationService,
                eventProducer,
                transactionManager
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private CreateSettlementJobRequest createRequest() {

        CreateSettlementJobRequest request =
                new CreateSettlementJobRequest();

        request.setTransactionIds(List.of(TRANSACTION_ID));

        return request;
    }

    private SettlementJob createJob() {

        SettlementJob job = new SettlementJob();

        job.setId(1L);
        job.setJobId(JOB_ID);
        job.setOrgId(ORG_ID);
        job.setStatus(SettlementJobStatus.PENDING);
        job.setTotalTransactions(1);
        job.setProcessedCount(0);
        job.setDiscrepancyCount(0);

        return job;
    }

    // --------------------------------------------------
    // JOB CREATION
    // --------------------------------------------------

    @Test
    void shouldCreateSettlementJobSuccessfully() {

        when(transactionRepository.findByIdAndOrgId(
                TRANSACTION_ID, ORG_ID))
                .thenReturn(Optional.of(mock(Transaction.class)));

        when(jobRepository.save(any(SettlementJob.class)))
                .thenAnswer(invocation -> {
                    SettlementJob job = invocation.getArgument(0);
                    job.setId(1L);
                    return job;
                });

        when(jobTransactionRepository.save(
                any(SettlementJobTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SettlementJob result =
                service.createSettlementJob(createRequest());

        assertNotNull(result);
        assertNotNull(result.getJobId());
        assertEquals(ORG_ID, result.getOrgId());
        assertEquals(SettlementJobStatus.PENDING, result.getStatus());
        assertEquals(1, result.getTotalTransactions());
        assertEquals(0, result.getProcessedCount());

        verify(transactionRepository)
                .findByIdAndOrgId(TRANSACTION_ID, ORG_ID);

        verify(jobRepository).save(any(SettlementJob.class));

        verify(jobTransactionRepository)
                .save(any(SettlementJobTransaction.class));
    }

    @Test
    void shouldRejectEmptyTransactionList() {

        CreateSettlementJobRequest request =
                new CreateSettlementJobRequest();

        request.setTransactionIds(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSettlementJob(request)
        );

        verifyNoInteractions(jobRepository);
        verifyNoInteractions(jobTransactionRepository);
    }

    @Test
    void shouldRejectDuplicateTransactionIds() {

        CreateSettlementJobRequest request =
                new CreateSettlementJobRequest();

        request.setTransactionIds(List.of(100L, 100L));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSettlementJob(request)
        );

        verifyNoInteractions(jobRepository);
    }

    @Test
    void shouldRejectTransactionNotBelongingToOrganization() {

        when(transactionRepository.findByIdAndOrgId(
                TRANSACTION_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSettlementJob(createRequest())
        );

        verify(jobRepository, never())
                .save(any(SettlementJob.class));
    }

    @Test
    void shouldRejectMissingTenantContext() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> service.createSettlementJob(createRequest())
        );

        verifyNoInteractions(jobRepository);
    }

    // --------------------------------------------------
    // JOB RETRIEVAL
    // --------------------------------------------------

    @Test
    void shouldReturnSettlementJobByJobId() {

        SettlementJob job = createJob();

        when(jobRepository.findByJobIdAndOrgId(JOB_ID, ORG_ID))
                .thenReturn(Optional.of(job));

        SettlementJob result = service.getSettlementJob(JOB_ID);

        assertEquals(JOB_ID, result.getJobId());
        assertEquals(ORG_ID, result.getOrgId());
    }

    @Test
    void shouldRejectUnknownSettlementJob() {

        when(jobRepository.findByJobIdAndOrgId(JOB_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getSettlementJob(JOB_ID)
        );
    }

    @Test
    void shouldReturnPaginatedSettlementJobs() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<SettlementJob> expected =
                new PageImpl<>(List.of(createJob()), pageable, 1);

        when(jobRepository.findByOrgId(ORG_ID, pageable))
                .thenReturn(expected);

        Page<SettlementJob> result =
                service.getSettlementJobs(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(JOB_ID, result.getContent().get(0).getJobId());

        verify(jobRepository).findByOrgId(ORG_ID, pageable);
    }

    // --------------------------------------------------
    // SUCCESSFUL EXECUTION
    // --------------------------------------------------

    @Test
    void shouldExecuteSettlementJobSuccessfully() {

        SettlementJob job = createJob();

        SettlementJobTransaction mapping =
                new SettlementJobTransaction();

        mapping.setId(1L);
        mapping.setSettlementJobId(1L);
        mapping.setTransactionId(TRANSACTION_ID);

        SettlementJobExecution execution =
                new SettlementJobExecution();

        when(jobRepository.claimPendingJob(
                eq(JOB_ID),
                eq(ORG_ID),
                eq(SettlementJobStatus.PENDING),
                eq(SettlementJobStatus.RUNNING),
                any()))
                .thenReturn(1);

        when(jobRepository.findByJobIdAndOrgId(JOB_ID, ORG_ID))
                .thenReturn(Optional.of(job));

        when(jobTransactionRepository
                .findBySettlementJobIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(mapping));

        when(executionRepository.countBySettlementJobId(1L))
                .thenReturn(0L);

        when(executionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution saved =
                            invocation.getArgument(0);
                    saved.setId(50L);
                    return saved;
                });

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(executionRepository.findById(50L))
                .thenReturn(Optional.of(execution));

        ReconciliationRecord record = new ReconciliationRecord();
        record.setTransactionId(TRANSACTION_ID);
        record.setOrgId(ORG_ID);
        record.setStatus(ReconciliationStatus.MATCHED);

        when(reconciliationService.reconcileTransaction(
                TRANSACTION_ID, ORG_ID))
                .thenReturn(record);

        SettlementJob result =
                service.executeSettlementJob(JOB_ID);

        assertEquals(SettlementJobStatus.COMPLETED, result.getStatus());
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, result.getDiscrepancyCount());

        assertEquals(
                SettlementExecutionStatus.COMPLETED,
                execution.getStatus()
        );

        verify(reconciliationService)
                .reconcileTransaction(TRANSACTION_ID, ORG_ID);

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.STARTED
        ));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.COMPLETED
        ));
    }

    // --------------------------------------------------
    // EXECUTION FAILURE
    // --------------------------------------------------

    @Test
    void shouldRejectExecutionWhenJobCannotBeClaimed() {

        when(jobRepository.claimPendingJob(
                eq(JOB_ID),
                eq(ORG_ID),
                eq(SettlementJobStatus.PENDING),
                eq(SettlementJobStatus.RUNNING),
                any()))
                .thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> service.executeSettlementJob(JOB_ID)
        );

        verifyNoInteractions(reconciliationService);
    }

    @Test
    void shouldMarkJobFailedWhenReconciliationFails() {

        SettlementJob job = createJob();

        SettlementJobTransaction mapping =
                new SettlementJobTransaction();

        mapping.setId(1L);
        mapping.setSettlementJobId(1L);
        mapping.setTransactionId(TRANSACTION_ID);

        SettlementJobExecution execution =
                new SettlementJobExecution();

        when(jobRepository.claimPendingJob(
                eq(JOB_ID),
                eq(ORG_ID),
                eq(SettlementJobStatus.PENDING),
                eq(SettlementJobStatus.RUNNING),
                any()))
                .thenReturn(1);

        when(jobRepository.findByJobIdAndOrgId(JOB_ID, ORG_ID))
                .thenReturn(Optional.of(job));

        when(jobTransactionRepository
                .findBySettlementJobIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(mapping));

        when(executionRepository.countBySettlementJobId(1L))
                .thenReturn(0L);

        when(executionRepository.save(any(SettlementJobExecution.class)))
                .thenAnswer(invocation -> {
                    SettlementJobExecution saved =
                            invocation.getArgument(0);
                    saved.setId(50L);
                    return saved;
                });

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(executionRepository.findById(50L))
                .thenReturn(Optional.of(execution));

        when(reconciliationService.reconcileTransaction(
                TRANSACTION_ID, ORG_ID))
                .thenThrow(new IllegalStateException(
                        "Reconciliation failed"));

        assertThrows(
                IllegalStateException.class,
                () -> service.executeSettlementJob(JOB_ID)
        );

        assertEquals(SettlementJobStatus.FAILED, job.getStatus());

        assertEquals(
                SettlementExecutionStatus.FAILED,
                execution.getStatus()
        );

        assertEquals("Reconciliation failed", job.getErrorMessage());

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.FAILED
        ));
    }

    // --------------------------------------------------
    // EXECUTION HISTORY
    // --------------------------------------------------

    @Test
    void shouldReturnExecutionHistoryForOrganizationJob() {

        SettlementJob job = createJob();

        SettlementJobExecution execution =
                new SettlementJobExecution();

        execution.setId(50L);
        execution.setSettlementJobId(1L);
        execution.setAttemptNumber(1);
        execution.setStatus(SettlementExecutionStatus.COMPLETED);

        when(jobRepository.findByJobIdAndOrgId(JOB_ID, ORG_ID))
                .thenReturn(Optional.of(job));

        when(executionRepository
                .findBySettlementJobIdOrderByAttemptNumberDesc(1L))
                .thenReturn(List.of(execution));

        List<SettlementJobExecution> history =
                service.getExecutionHistory(JOB_ID);

        assertEquals(1, history.size());
        assertEquals(1, history.get(0).getAttemptNumber());

        verify(executionRepository)
                .findBySettlementJobIdOrderByAttemptNumberDesc(1L);
    }
}