package com.payflow.payflow_backend.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaConfigTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9094";

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig(BOOTSTRAP_SERVERS);
    }

    @Test
    void transactionEventsShouldRouteToTransactionEventsDlt() {

        TopicPartition destination =
                kafkaConfig.resolveDltDestination(
                        "transaction-events",
                        1
                );

        assertEquals("transaction-events-dlt", destination.topic());
        assertEquals(1, destination.partition());
    }

    @Test
    void transactionEventsDltShouldRouteToFailureDlt() {

        TopicPartition destination =
                kafkaConfig.resolveDltDestination(
                        "transaction-events-dlt",
                        2
                );

        assertEquals(
                "transaction-events-dlt.DLT",
                destination.topic()
        );
        assertEquals(2, destination.partition());
    }

    @Test
    void otherTopicsShouldRouteToTheirDefaultDlt() {

        TopicPartition destination =
                kafkaConfig.resolveDltDestination(
                        "payment-events",
                        0
                );

        assertEquals("payment-events.DLT", destination.topic());
        assertEquals(0, destination.partition());
    }

    @Test
    void recovererShouldPublishRecordToTransactionEventsDlt()
            throws Exception {

        String groupId = "dlt-integration-test-" + UUID.randomUUID();
        String eventKey = "dlt-test-" + UUID.randomUUID();
        String eventValue = "{\"test\":\"dlt-publishing\"}";

        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS
        );
        producerProperties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        producerProperties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        DefaultKafkaProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProperties);

        try {
            KafkaTemplate<String, String> template =
        new KafkaTemplate<>(producerFactory);
            DeadLetterPublishingRecoverer recoverer =
                    new DeadLetterPublishingRecoverer(
                            template,
                            (record, exception) ->
                                    kafkaConfig.resolveDltDestination(
                                            record.topic(),
                                            record.partition()
                                    )
                    );

            Map<String, Object> consumerProperties = new HashMap<>();

            consumerProperties.put(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    BOOTSTRAP_SERVERS
            );
            consumerProperties.put(
                    ConsumerConfig.GROUP_ID_CONFIG,
                    groupId
            );
            consumerProperties.put(
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
             "earliest"
            );
            consumerProperties.put(
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                    false
            );
            consumerProperties.put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class
            );
            consumerProperties.put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class
            );

            try (KafkaConsumer<String, String> consumer =
                         new KafkaConsumer<>(consumerProperties)) {

                consumer.subscribe(
                        Collections.singletonList("transaction-events-dlt")
                );

                // Wait for Kafka to assign a partition.
                long assignmentDeadline =
                        System.currentTimeMillis() + 10_000;

                while (consumer.assignment().isEmpty()
                        && System.currentTimeMillis() < assignmentDeadline) {

                    consumer.poll(Duration.ofMillis(100));
                }

                assertFalse(
                        consumer.assignment().isEmpty(),
                        "Consumer should be assigned a partition"
                );

                // Simulate a record that failed during processing.
                ConsumerRecord<String, String> failedRecord =
                        new ConsumerRecord<>(
                                "transaction-events",
                                1,
                                0L,
                                eventKey,
                                eventValue
                        );

                // Invoke the actual Spring Kafka recoverer.
                recoverer.accept(
                        failedRecord,
                        null,
                        new RuntimeException(
                                "Simulated processing failure"
                        )
                );

                // Verify the record arrives in the DLT.
                boolean received = false;
                long receiveDeadline =
                        System.currentTimeMillis() + 10_000;

                while (!received
                        && System.currentTimeMillis() < receiveDeadline) {

                    var records = consumer.poll(Duration.ofMillis(250));

                    for (ConsumerRecord<String, String> record : records) {

                        if (eventKey.equals(record.key())) {

                            assertEquals(
                                    "transaction-events-dlt",
                                    record.topic()
                            );

                            assertEquals(1, record.partition());
                            assertEquals(eventValue, record.value());

                            received = true;
                            break;
                        }
                    }
                }

                assertTrue(
                        received,
                        "The failed record should be published to the DLT"
                );
            }

        } finally {
            producerFactory.destroy();
        }
    }
}