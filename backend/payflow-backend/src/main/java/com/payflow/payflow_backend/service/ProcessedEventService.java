package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.repository.ProcessedEventRepository;
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

    @Transactional
    public boolean tryMarkAsProcessed(
            UUID eventId,
            String eventType) {

        int insertedRows =
                processedEventRepository.insertIfNotExists(
                        eventId,
                        eventType,
                        LocalDateTime.now());

        return insertedRows == 1;
    }
}