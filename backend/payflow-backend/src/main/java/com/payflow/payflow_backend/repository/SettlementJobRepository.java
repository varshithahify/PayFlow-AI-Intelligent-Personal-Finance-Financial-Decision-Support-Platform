package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.SettlementJob;
import com.payflow.payflow_backend.entity.SettlementJobStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SettlementJobRepository
        extends JpaRepository<SettlementJob, Long>,
                JpaSpecificationExecutor<SettlementJob> {

    Optional<SettlementJob> findByJobIdAndOrgId(
            String jobId,
            Long orgId
    );

    Page<SettlementJob> findByOrgId(
            Long orgId,
            Pageable pageable
    );

    Page<SettlementJob> findByOrgIdAndStatus(
            Long orgId,
            SettlementJobStatus status,
            Pageable pageable
    );

    long countByOrgId(Long orgId);

    long countByOrgIdAndStatus(
            Long orgId,
            SettlementJobStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SettlementJob j
            SET j.status = :runningStatus,
                j.startedAt = :startedAt,
                j.errorMessage = null
            WHERE j.jobId = :jobId
              AND j.orgId = :orgId
              AND j.status = :pendingStatus
            """)
    int claimPendingJob(
            @Param("jobId") String jobId,
            @Param("orgId") Long orgId,
            @Param("pendingStatus") SettlementJobStatus pendingStatus,
            @Param("runningStatus") SettlementJobStatus runningStatus,
            @Param("startedAt") LocalDateTime startedAt
    );
}
