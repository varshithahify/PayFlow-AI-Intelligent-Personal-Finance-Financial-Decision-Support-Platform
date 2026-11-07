package com.payflow.payflow_backend.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.config.KafkaConfig;
import com.payflow.payflow_backend.entity.TransactionStatus;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {
        KafkaConfig.class,
        TransactionEventProducer.class
})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9094"
})
class TransactionEventProducerIntegrationTest {

    private static final String TOPIC = "transaction-events";

    @Autowired
private TransactionEventProducer producer;

private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPublishTransactionEventToKafka() throws Exception {

        UUID eventId = UUID.randomUUID();

        TransactionEvent event = new TransactionEvent(
                987654L,
                1001L,
                1L,
                new BigDecimal("2500.00"),
                "INR",
                "UPI",
                TransactionStatus.CREATED,
                TransactionEventType.CREATED,
                LocalDateTime.now()
        );

        event.setEventId(eventId);

        Properties properties = new Properties();

        properties.put(
                "bootstrap.servers",
                "localhost:9094"
        );

        properties.put(
                "group.id",
                "transaction-producer-integration-" + eventId
        );

        properties.put(
                "key.deserializer",
                StringDeserializer.class.getName()
        );

        properties.put(
                "value.deserializer",
                StringDeserializer.class.getName()
        );

        properties.put(
                "auto.offset.reset",
                "latest"
        );

        properties.put(
                "enable.auto.commit",
                "false"
        );

        try (Consumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(Collections.singletonList(TOPIC));

            // Establish the consumer subscription before publishing.
            consumer.poll(Duration.ofSeconds(2));

            // Publish through the actual application producer.
            producer.publish(event).get();

            ConsumerRecord<String, String> received = null;

            long deadline = System.currentTimeMillis() + 15000;

            while (System.currentTimeMillis() < deadline
                    && received == null) {

                var records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {

                    JsonNode json =
                            objectMapper.readTree(record.value());

                    if (eventId.toString().equals(
                            json.path("eventId").asText())) {

                        received = record;
                        break;
                    }
                }
            }

            assertNotNull(
                    received,
                    "Transaction event was not received from Kafka"
            );

            assertEquals("987654", received.key());

            JsonNode actual =
                    objectMapper.readTree(received.value());

            assertEquals(eventId.toString(),
                    actual.path("eventId").asText());

            assertEquals(987654L,
                    actual.path("transactionId").asLong());

            assertEquals(1001L,
                    actual.path("userId").asLong());

            assertEquals(1L,
                    actual.path("orgId").asLong());

            assertEquals(
                    0,
                    new BigDecimal("2500.00").compareTo(
                            actual.path("amount").decimalValue()
                    )
            );

            assertEquals("INR",
                    actual.path("currency").asText());

            assertEquals("UPI",
                    actual.path("paymentMethod").asText());

            assertEquals("CREATED",
                    actual.path("status").asText());

            assertEquals("CREATED",
                    actual.path("eventType").asText());

            assertTrue(actual.hasNonNull("timestamp"));
        }
    }
}
