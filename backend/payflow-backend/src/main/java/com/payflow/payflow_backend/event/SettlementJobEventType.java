package com.payflow.payflow_backend.event;

/**
 * Lifecycle events emitted by a settlement job.
 */
public enum SettlementJobEventType {

    STARTED,

    COMPLETED,

    FAILED,

    DISCREPANCY_DETECTED
}