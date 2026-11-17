package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.CreateSettlementJobRequest;
import com.payflow.payflow_backend.entity.*;
import com.payflow.payflow_backend.event.*;
import com.payflow.payflow_backend.repository.*;
import com.payflow.payflow_backend.security.TenantContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementJobManagementService {

    private static final Logger log =
            LoggerFactory.getLogger(SettlementJobManagementService.class);

    private final SettlementJobRepository jobRepository;
    private final SettlementJobTransactionRepository jobTransactionRepository;
    private final SettlementJobExecutionRepository executionRepository;
    private final TransactionRepository transactionRepository;
    private final ReconciliationService reconciliationService;
    private final SettlementJobEventProducer eventProducer;
    private final TransactionTemplate transactionTemplate;

    public SettlementJobManagementService(
            SettlementJobRepository jobRepository,
            SettlementJobTransactionRepository jobTransactionRepository,
            SettlementJobExecutionRepository executionRepository,
            TransactionRepository transactionRepository,
            ReconciliationService reconciliationService,
            SettlementJobEventProducer eventProducer,
            PlatformTransactionManager transactionManager) {

        this.jobRepository = jobRepository;
        this.jobTransactionRepository = jobTransactionRepository;
        this.executionRepository = executionRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationService = reconciliationService;
        this.eventProducer = eventProducer;

        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // --------------------------------------------------
    // CREATE SETTLEMENT JOB
    // --------------------------------------------------

    public SettlementJob createSettlementJob(
            CreateSettlementJobRequest request) {

        Long orgId = TenantContext.requireOrgId();

        if (request == null
                || request.getTransactionIds() == null
                || request.getTransactionIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "Transaction IDs must not be empty");
        }

        List<Long> transactionIds = request.getTransactionIds();

        if (transactionIds.stream().anyMatch(
                id -> id == null || id <= 0)) {
            throw new IllegalArgumentException(
                    "Transaction IDs must be positive");
        }

        if (new HashSet<>(transactionIds).size()
                != transactionIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate transaction IDs are not allowed");
        }

        return transactionTemplate.execute(status -> {

            for (Long transactionId : transactionIds) {
                transactionRepository.findByIdAndOrgId(
                        transactionId,
                        orgId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found: " + transactionId));
            }

            SettlementJob job = new SettlementJob();

            job.setJobId(UUID.randomUUID().toString());
            job.setOrgId(orgId);
            job.setStatus(SettlementJobStatus.PENDING);
            job.setTotalTransactions(transactionIds.size());
            job.setProcessedCount(0);
            job.setDiscrepancyCount(0);

            SettlementJob savedJob = jobRepository.save(job);

            for (Long transactionId : transactionIds) {

                SettlementJobTransaction mapping =
                        new SettlementJobTransaction();

                mapping.setSettlementJobId(savedJob.getId());
                mapping.setTransactionId(transactionId);

                jobTransactionRepository.save(mapping);
            }

            return savedJob;
        });
    }

    // --------------------------------------------------
    // GET PAGINATED JOBS
    // --------------------------------------------------

    public Page<SettlementJob> getSettlementJobs(
            Pageable pageable) {

        Long orgId = TenantContext.requireOrgId();

        return jobRepository.findByOrgId(orgId, pageable);
    }

    public Page<SettlementJob> getSettlementJobs(
            SettlementJobStatus status,
            Pageable pageable) {

        Long orgId = TenantContext.requireOrgId();

        if (status == null) {
            return jobRepository.findByOrgId(orgId, pageable);
        }

        return jobRepository.findByOrgIdAndStatus(
                orgId,
                status,
                pageable
        );
    }

    // --------------------------------------------------
    // GET JOB DETAILS
    // --------------------------------------------------

    public SettlementJob getSettlementJob(String jobId) {

        Long orgId = TenantContext.requireOrgId();

        return jobRepository.findByJobIdAndOrgId(jobId, orgId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Settlement job not found"));
    }

    // --------------------------------------------------
    // GET EXECUTION HISTORY
    // --------------------------------------------------

    public List<SettlementJobExecution> getExecutionHistory(
            String jobId) {

        SettlementJob job = getSettlementJob(jobId);

        return executionRepository
                .findBySettlementJobIdOrderByAttemptNumberDesc(
                        job.getId());
    }

    // --------------------------------------------------
    // EXECUTE SETTLEMENT JOB
    // --------------------------------------------------

    public SettlementJob executeSettlementJob(String jobId) {

        Long orgId = TenantContext.requireOrgId();

        ExecutionContext context = transactionTemplate.execute(status -> {

            int claimed = jobRepository.claimPendingJob(
                    jobId,
                    orgId,
                    SettlementJobStatus.PENDING,
                    SettlementJobStatus.RUNNING,
                    LocalDateTime.now()
            );

            if (claimed != 1) {
                throw new IllegalStateException(
                        "Settlement job is not pending or is already executing");
            }

            SettlementJob job = jobRepository
                    .findByJobIdAndOrgId(jobId, orgId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Settlement job not found"));

            List<SettlementJobTransaction> mappings =
                    jobTransactionRepository
                            .findBySettlementJobIdOrderByCreatedAtAsc(
                                    job.getId());

            int attemptNumber = Math.toIntExact(
                    executionRepository.countBySettlementJobId(job.getId()) + 1
            );

            SettlementJobExecution execution =
                    new SettlementJobExecution();

            execution.setSettlementJobId(job.getId());
            execution.setAttemptNumber(attemptNumber);
            execution.setStatus(SettlementExecutionStatus.RUNNING);
            execution.setProcessedCount(0);
            execution.setDiscrepancyCount(0);
            execution.setStartedAt(LocalDateTime.now());

            executionRepository.save(execution);

            return new ExecutionContext(
                    job,
                    execution,
                    mappings.stream()
                            .map(SettlementJobTransaction::getTransactionId)
                            .toList()
            );
        });

        publishEvent(
                jobId,
                orgId,
                SettlementJobEventType.STARTED,
                0,
                0,
                "Settlement job started"
        );

        int processedCount = 0;
        int discrepancyCount = 0;

        try {

            for (Long transactionId : context.transactionIds) {

                ReconciliationRecord record =
                        reconciliationService.reconcileTransaction(
                                transactionId,
                                orgId
                        );

                if (record == null) {
                    throw new IllegalStateException(
                            "Reconciliation returned no record for transaction "
                                    + transactionId);
                }

                processedCount++;

                if (record.getStatus() != ReconciliationStatus.MATCHED) {
                    discrepancyCount++;

                    publishEvent(
                            jobId,
                            orgId,
                            SettlementJobEventType.DISCREPANCY_DETECTED,
                            processedCount,
                            discrepancyCount,
                            "Discrepancy detected for transaction "
                                    + transactionId
                                    + ": "
                                    + record.getStatus()
                    );
                }

                updateProgress(
                        context,
                        processedCount,
                        discrepancyCount
                );
            }

            completeExecution(context, processedCount, discrepancyCount);

            publishEvent(
                    jobId,
                    orgId,
                    SettlementJobEventType.COMPLETED,
                    processedCount,
                    discrepancyCount,
                    "Settlement job completed successfully"
            );

            log.info(
                    "Settlement job completed: jobId={}, orgId={}, processed={}, discrepancies={}",
                    jobId,
                    orgId,
                    processedCount,
                    discrepancyCount
            );

            return getSettlementJob(jobId);

        } catch (RuntimeException exception) {

            failExecution(
                    context,
                    processedCount,
                    discrepancyCount,
                    exception
            );

            publishEvent(
                    jobId,
                    orgId,
                    SettlementJobEventType.FAILED,
                    processedCount,
                    discrepancyCount,
                    "Settlement job failed: " + exception.getMessage()
            );

            log.error(
                    "Settlement job failed: jobId={}, processed={}",
                    jobId,
                    processedCount,
                    exception
            );

            throw exception;
        }
    }

    // --------------------------------------------------
    // UPDATE EXECUTION PROGRESS
    // --------------------------------------------------

    private void updateProgress(
            ExecutionContext context,
            int processed,
            int discrepancies) {

        transactionTemplate.executeWithoutResult(status -> {

            SettlementJob job = jobRepository.findById(
                    context.job.getId()
            ).orElseThrow();

            SettlementJobExecution execution =
                    executionRepository.findById(
                            context.execution.getId()
                    ).orElseThrow();

            job.setProcessedCount(processed);
            job.setDiscrepancyCount(discrepancies);

            execution.setProcessedCount(processed);
            execution.setDiscrepancyCount(discrepancies);

            jobRepository.save(job);
            executionRepository.save(execution);
        });
    }

    // --------------------------------------------------
    // COMPLETE EXECUTION
    // --------------------------------------------------

    private void completeExecution(
            ExecutionContext context,
            int processed,
            int discrepancies) {

        transactionTemplate.executeWithoutResult(status -> {

            LocalDateTime completedAt = LocalDateTime.now();

            SettlementJob job = jobRepository.findById(
                    context.job.getId()
            ).orElseThrow();

            SettlementJobExecution execution =
                    executionRepository.findById(
                            context.execution.getId()
                    ).orElseThrow();

            job.setStatus(SettlementJobStatus.COMPLETED);
            job.setProcessedCount(processed);
            job.setDiscrepancyCount(discrepancies);
            job.setCompletedAt(completedAt);
            job.setErrorMessage(null);

            execution.setStatus(SettlementExecutionStatus.COMPLETED);
            execution.setProcessedCount(processed);
            execution.setDiscrepancyCount(discrepancies);
            execution.setCompletedAt(completedAt);

            jobRepository.save(job);
            executionRepository.save(execution);
        });
    }

    // --------------------------------------------------
    // FAIL EXECUTION
    // --------------------------------------------------

    private void failExecution(
            ExecutionContext context,
            int processed,
            int discrepancies,
            RuntimeException exception) {

        transactionTemplate.executeWithoutResult(status -> {

            LocalDateTime completedAt = LocalDateTime.now();

            SettlementJob job = jobRepository.findById(
                    context.job.getId()
            ).orElseThrow();

            SettlementJobExecution execution =
                    executionRepository.findById(
                            context.execution.getId()
                    ).orElseThrow();

            String error = exception.getMessage();

            if (error == null || error.isBlank()) {
                error = "Unexpected settlement execution failure";
            }

            if (error.length() > 2000) {
                error = error.substring(0, 2000);
            }

            job.setStatus(SettlementJobStatus.FAILED);
            job.setProcessedCount(processed);
            job.setDiscrepancyCount(discrepancies);
            job.setErrorMessage(error);
            job.setCompletedAt(completedAt);

            execution.setStatus(SettlementExecutionStatus.FAILED);
            execution.setProcessedCount(processed);
            execution.setDiscrepancyCount(discrepancies);
            execution.setErrorMessage(error);
            execution.setCompletedAt(completedAt);

            jobRepository.save(job);
            executionRepository.save(execution);
        });
    }

    // --------------------------------------------------
    // KAFKA EVENT PUBLISHING
    // --------------------------------------------------

    private void publishEvent(
            String jobId,
            Long orgId,
            SettlementJobEventType eventType,
            int processed,
            int discrepancies,
            String details) {

        SettlementJobEvent event = new SettlementJobEvent(
                UUID.randomUUID(),
                jobId,
                orgId,
                eventType,
                processed,
                discrepancies,
                details,
                LocalDateTime.now()
        );

        try {
            eventProducer.publish(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Unable to initiate Kafka event publishing: jobId={}, eventType={}",
                    jobId,
                    eventType,
                    exception
            );
        }
    }

    // --------------------------------------------------
    // EXECUTION CONTEXT
    // --------------------------------------------------

    private static class ExecutionContext {

        private final SettlementJob job;
        private final SettlementJobExecution execution;
        private final List<Long> transactionIds;

        private ExecutionContext(
                SettlementJob job,
                SettlementJobExecution execution,
                List<Long> transactionIds) {

            this.job = job;
            this.execution = execution;
            this.transactionIds = transactionIds;
        }
    }
}