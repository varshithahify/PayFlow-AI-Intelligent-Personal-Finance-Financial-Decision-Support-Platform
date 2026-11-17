package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.CreateSettlementJobRequest;
import com.payflow.payflow_backend.entity.SettlementJob;
import com.payflow.payflow_backend.entity.SettlementJobExecution;
import com.payflow.payflow_backend.entity.SettlementJobStatus;
import com.payflow.payflow_backend.service.SettlementJobManagementService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlement-jobs")
@Validated
public class SettlementJobController {

    private final SettlementJobManagementService settlementJobService;

    public SettlementJobController(
            SettlementJobManagementService settlementJobService) {
        this.settlementJobService = settlementJobService;
    }

    // --------------------------------------------------
    // CREATE SETTLEMENT JOB
    // POST /api/settlement-jobs
    // --------------------------------------------------

    @PostMapping
    public ResponseEntity<SettlementJob> createSettlementJob(
            @Valid @RequestBody CreateSettlementJobRequest request) {

        SettlementJob job =
                settlementJobService.createSettlementJob(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(job);
    }

    // --------------------------------------------------
    // GET ALL SETTLEMENT JOBS
    // GET /api/settlement-jobs
    // --------------------------------------------------

    @GetMapping
    public ResponseEntity<Page<SettlementJob>> getSettlementJobs(
            @RequestParam(defaultValue = "0")
            @Min(0) int page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,

            @RequestParam(required = false)
            SettlementJobStatus status) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SettlementJob> jobs =
                settlementJobService.getSettlementJobs(
                        status,
                        pageable
                );

        return ResponseEntity.ok(jobs);
    }

    // --------------------------------------------------
    // GET SETTLEMENT JOB DETAILS
    // GET /api/settlement-jobs/{jobId}
    // --------------------------------------------------

    @GetMapping("/{jobId}")
    public ResponseEntity<SettlementJob> getSettlementJob(
            @PathVariable String jobId) {

        return ResponseEntity.ok(
                settlementJobService.getSettlementJob(jobId)
        );
    }

    // --------------------------------------------------
    // EXECUTE SETTLEMENT JOB
    // POST /api/settlement-jobs/{jobId}/execute
    // --------------------------------------------------

    @PostMapping("/{jobId}/execute")
    public ResponseEntity<SettlementJob> executeSettlementJob(
            @PathVariable String jobId) {

        SettlementJob job =
                settlementJobService.executeSettlementJob(jobId);

        return ResponseEntity.ok(job);
    }

    // --------------------------------------------------
    // GET EXECUTION HISTORY
    // GET /api/settlement-jobs/{jobId}/history
    // --------------------------------------------------

    @GetMapping("/{jobId}/history")
    public ResponseEntity<List<SettlementJobExecution>> getExecutionHistory(
            @PathVariable String jobId) {

        return ResponseEntity.ok(
                settlementJobService.getExecutionHistory(jobId)
        );
    }
}