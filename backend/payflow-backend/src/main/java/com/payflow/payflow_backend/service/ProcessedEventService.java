package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ProcessedEvent;
import com.payflow.payflow_backend.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(
            ProcessedEventRepository processedEventRepository) {

        this.processedEventRepository =
                processedEventRepository;
    }

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(UUID eventId) {

        return processedEventRepository
                .existsByEventId(eventId);
    }

    @Transactional
    public boolean tryMarkAsProcessed(
            UUID eventId,
            String eventType) {

        if (alreadyProcessed(eventId)) {
            return false;
        }

        try {
            ProcessedEvent processedEvent =
                    new ProcessedEvent(
                            eventId,
                            eventType,
                            LocalDateTime.now());

            processedEventRepository.saveAndFlush(
                    processedEvent);

            return true;

        } catch (DataIntegrityViolationException exception) {

            return false;
        }
    }
}