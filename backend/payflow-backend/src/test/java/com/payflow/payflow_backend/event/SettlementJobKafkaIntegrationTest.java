package com.payflow.payflow_backend.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementJobKafkaIntegrationTest {
private static final String BOOTSTRAP_SERVERS = "localhost:9094";
private static final String TOPIC = "settlement-jobs";

@Test
void shouldPublishAndConsumeSettlementEventThroughRealKafka()
        throws Exception {

    String uniqueJobId = "integration-" + UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    SettlementJobEvent event = new SettlementJobEvent(
            eventId,
            uniqueJobId,
            10L,
            SettlementJobEventType.COMPLETED,
            5,
            2,
            "Integration test settlement completed",
            LocalDateTime.now()
    );

    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            BOOTSTRAP_SERVERS
    );
    producerConfig.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
    );
    producerConfig.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JacksonJsonSerializer.class
    );

    DefaultKafkaProducerFactory<String, Object> producerFactory =
            new DefaultKafkaProducerFactory<>(producerConfig);

    KafkaTemplate<String, Object> kafkaTemplate =
            new KafkaTemplate<String, Object>(producerFactory);

    try {
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS
        );
        consumerConfig.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "settlement-integration-" + UUID.randomUUID()
        );
        consumerConfig.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        consumerConfig.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        consumerConfig.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );
        consumerConfig.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        Properties consumerProperties = new Properties();
        consumerConfig.forEach(consumerProperties::put);

        try (Consumer<String, String> consumer =
                     new KafkaConsumer<>(consumerProperties)) {

            consumer.subscribe(Collections.singletonList(TOPIC));

            long assignmentDeadline =
                    System.currentTimeMillis() + 15_000;

            while (consumer.assignment().isEmpty()
                    && System.currentTimeMillis() < assignmentDeadline) {
                consumer.poll(Duration.ofMillis(200));
            }

            assertTrue(
                    !consumer.assignment().isEmpty(),
                    "Consumer should receive a partition assignment"
            );

            var sendResult = kafkaTemplate.send(TOPIC, uniqueJobId, event)
                    .get(15, TimeUnit.SECONDS);

            System.out.println("KAFKA TEST: Message sent successfully");
            System.out.println("KAFKA TEST: Topic = " + sendResult.getRecordMetadata().topic());
            System.out.println("KAFKA TEST: Partition = " + sendResult.getRecordMetadata().partition());
            System.out.println("KAFKA TEST: Offset = " + sendResult.getRecordMetadata().offset());
            System.out.println("KAFKA TEST: Key = " + uniqueJobId);

            String receivedPayload = null;
            long messageDeadline =
                    System.currentTimeMillis() + 15_000;

            while (receivedPayload == null
                    && System.currentTimeMillis() < messageDeadline) {

                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(500));

                for (var record : records) {
                    if (uniqueJobId.equals(record.key())) {
                        receivedPayload = record.value();
                        break;
                    }
                }
            }

            assertNotNull(
                    receivedPayload,
                    "Settlement event should be received from Kafka"
            );

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            SettlementJobEvent receivedEvent =
                    objectMapper.readValue(
                            receivedPayload,
                            SettlementJobEvent.class
                    );

            assertEquals(eventId, receivedEvent.getEventId());
            assertEquals(uniqueJobId, receivedEvent.getJobId());
            assertEquals(10L, receivedEvent.getOrgId());
            assertEquals(
                    SettlementJobEventType.COMPLETED,
                    receivedEvent.getEventType()
            );
            assertEquals(5, receivedEvent.getTransactionCount());
            assertEquals(2, receivedEvent.getDiscrepancyCount());
            assertEquals(
                    "Integration test settlement completed",
                    receivedEvent.getDetails()
            );
            assertNotNull(receivedEvent.getOccurredAt());
        }
    } finally {
        kafkaTemplate.destroy();
        producerFactory.destroy();
    }
}
}

