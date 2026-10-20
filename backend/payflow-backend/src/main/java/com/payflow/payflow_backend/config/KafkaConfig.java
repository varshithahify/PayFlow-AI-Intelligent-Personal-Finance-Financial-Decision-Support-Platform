package com.payflow.payflow_backend.config;

import com.payflow.payflow_backend.event.TransactionEvent;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private final String bootstrapServers;

    private static final String CONSUMER_GROUP = "payflow-group";

    private static final String TRANSACTION_EVENTS =
            "transaction-events";

    private static final String TRANSACTION_EVENTS_DLT =
            "transaction-events-dlt";

    private static final String TRANSACTION_EVENTS_DLT_FAILURE =
            "transaction-events-dlt.DLT";

    public KafkaConfig(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers) {

        this.bootstrapServers = bootstrapServers;
    }

    // -------------------------
    // Kafka Admin and Topics
    // -------------------------

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name("fraud-alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name("notification-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic settlementJobsTopic() {
        return TopicBuilder.name("settlement-jobs")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionEventsDltTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionEventsDltFailureTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_DLT_FAILURE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // -------------------------
    // Normal Event Producer
    // -------------------------

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonJsonSerializer.class
        );

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {

        return new KafkaTemplate<>(producerFactory);
    }

    // -------------------------
    // DLT Producer
    // -------------------------

   @Bean
public ProducerFactory<String, Object> dltProducerFactory() {
    Map<String, Object> config = new HashMap<>();

    config.put(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers
    );

    config.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
    );

    config.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            DelegatingByTypeSerializer.class
    );

    Map<Class<?>, org.apache.kafka.common.serialization.Serializer<?>>
            serializers = new LinkedHashMap<>();

    // Preserve raw byte[] payloads when forwarding failed records.
    serializers.put(
            byte[].class,
            new ByteArraySerializer()
    );

    // Serialize normal Java objects, including TransactionEvent.
    serializers.put(
            Object.class,
            new JacksonJsonSerializer<>()
    );

    DelegatingByTypeSerializer valueSerializer =
            new DelegatingByTypeSerializer(
                    serializers,
                    true
            );

    return new DefaultKafkaProducerFactory<>(
            config,
            new StringSerializer(),
            valueSerializer
    );
}

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
            ProducerFactory<String, Object> dltProducerFactory) {

        return new KafkaTemplate<>(dltProducerFactory);
    }

    // -------------------------
    // Consumer Configuration
    // -------------------------

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                CONSUMER_GROUP
        );

        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class
        );

        config.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JacksonJsonDeserializer.class.getName()
        );

        config.put(
                JacksonJsonDeserializer.TRUSTED_PACKAGES,
                "com.payflow.payflow_backend.event"
        );

        config.put(
                JacksonJsonDeserializer.VALUE_DEFAULT_TYPE,
                TransactionEvent.class.getName()
        );

        config.put(
                JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS,
                false
        );

        return new DefaultKafkaConsumerFactory<>(config);
    }

    // -------------------------
    // Dead Letter Publishing
    // -------------------------

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> dltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        dltKafkaTemplate,
                        (record, exception) -> resolveDltDestination(
                                record.topic(),
                                record.partition()
                        )
                );

        recoverer.setFailIfSendResultIsError(true);

        recoverer.setWaitForSendResultTimeout(
                Duration.ofSeconds(10)
        );

        return recoverer;
    }

    // -------------------------
    // DLT Destination Routing
    // -------------------------

    TopicPartition resolveDltDestination(
            String sourceTopic,
            int partition) {

        String destinationTopic;

        if (TRANSACTION_EVENTS.equals(sourceTopic)) {
            destinationTopic = TRANSACTION_EVENTS_DLT;

        } else if (TRANSACTION_EVENTS_DLT.equals(sourceTopic)) {
            destinationTopic = TRANSACTION_EVENTS_DLT_FAILURE;

        } else {
            destinationTopic = sourceTopic + ".DLT";
        }

        return new TopicPartition(destinationTopic, partition);
    }

    // -------------------------
    // Kafka Error Handling
    // -------------------------

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {

        FixedBackOff backOff = new FixedBackOff(2000L, 2L);

        return new DefaultErrorHandler(
                deadLetterPublishingRecoverer,
                backOff
        );
    }

    // -------------------------
    // Kafka Listener Container
    // -------------------------

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }
}