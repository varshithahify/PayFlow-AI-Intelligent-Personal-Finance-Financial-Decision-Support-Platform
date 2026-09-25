package com.payflow.payflow_backend.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final String BOOTSTRAP_SERVERS =
            "localhost:9092";

    private static final String CONSUMER_GROUP =
            "payflow-group";

    // -------------------------
    // Producer Configuration
    // -------------------------

    @Bean
    public ProducerFactory<String, Object> producerFactory() {

        Map<String, Object> config =
                new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);

        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonJsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {

        return new KafkaTemplate<>(producerFactory);
    }

    // -------------------------
    // Consumer Configuration
    // -------------------------

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {

        Map<String, Object> config =
                new HashMap<>();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);

        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                CONSUMER_GROUP);

        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        /*
         * Wrap the JSON deserializer with
         * ErrorHandlingDeserializer so that
         * deserialization failures can be
         * handled by Spring Kafka's error handler.
         */
        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        config.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JacksonJsonDeserializer.class.getName());

        /*
         * TransactionEvent belongs to this package.
         *
         * We trust only the event package instead
         * of trusting the entire application package.
         */
        config.put(
                JacksonJsonDeserializer.TRUSTED_PACKAGES,
                "com.payflow.payflow_backend.event");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    // -------------------------
    // Dead Letter Topic
    // -------------------------

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        /*
         * When all retry attempts are exhausted,
         * publish the failed Kafka record to a
         * Dead Letter Topic.
         *
         * Example:
         *
         * transaction-events
         *        ↓
         * transaction-events.DLT
         */
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate);
    }

    // -------------------------
    // Kafka Error Handling
    // -------------------------

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {

        /*
         * Initial processing attempt
         *        +
         * 2 additional retries
         *
         * Total attempts = 3
         *
         * Delay between attempts = 2 seconds.
         */
        FixedBackOff backOff =
                new FixedBackOff(
                        2000L,
                        2L);

        /*
         * If all attempts fail,
         * DeadLetterPublishingRecoverer publishes
         * the failed record to the DLT.
         */
        return new DefaultErrorHandler(
                deadLetterPublishingRecoverer,
                backOff);
    }

    // -------------------------
    // Kafka Listener Container
    // -------------------------

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, Object>
                factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                consumerFactory);

        factory.setCommonErrorHandler(
                kafkaErrorHandler);

        return factory;
    }
}