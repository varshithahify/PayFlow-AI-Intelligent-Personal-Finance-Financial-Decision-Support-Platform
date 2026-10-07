package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findByOrgIdOrderByCreatedAtDesc(Long orgId);

    Optional<WebhookConfig> findByOrgIdAndId(Long orgId, Long id);

    List<WebhookConfig> findByOrgIdAndActiveTrueOrderByCreatedAtDesc(Long orgId);
}