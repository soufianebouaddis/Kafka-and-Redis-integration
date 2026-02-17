package os.org.inventory_service.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.inventory-response}")
    private String inventoryResponseTopic;


    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,            bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,         StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,       JacksonJsonSerializer.class);

        // ── Reliability ──────────────────────────────────────
        // acks=all → leader + all ISR replicas must acknowledge
        props.put(ProducerConfig.ACKS_CONFIG,                         "all");
        // idempotent producer → exactly-once delivery, no duplicates on retry
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,           true);
        props.put(ProducerConfig.RETRIES_CONFIG,                      Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // ── Performance ──────────────────────────────────────
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,             "snappy");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,                   16_384);   // 16 KB
        props.put(ProducerConfig.LINGER_MS_CONFIG,                    10);       // wait 10 ms to batch
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG,                33_554_432); // 32 MB

        // ── Timeouts ─────────────────────────────────────────
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,           30_000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,          120_000);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ═══════════════════════════════════════════════════════════
    //  CONSUMER  (Order Service → Inventory)
    // ═══════════════════════════════════════════════════════════

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,             bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,        StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,      JacksonJsonDeserializer.class);

        // ── Deserialization ──────────────────────────────────
        // Trust all packages so we can deserialize OrderEvent DTOs
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,                   "*");
        // Don't rely on type headers — use VALUE_DEFAULT_TYPE per listener
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS,              false);

        // ── Consumer Group ───────────────────────────────────
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                      "inventory-service-group");
        // earliest → replay from beginning if the group has no committed offset
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,             "earliest");
        // We commit manually after processing — no silent loss on crash
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,            false);

        // ── Performance ──────────────────────────────────────
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,              500);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,          300_000); // 5 min
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,               1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,             500);

        // ── Reliability ──────────────────────────────────────
        // read_committed → skip uncommitted transactional messages
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,               "read_committed");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,            10_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,         3_000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Listener container factory.
     * - Manual ack mode (MANUAL)
     * - 3 concurrent consumer threads
     * - Retry twice on error, then stop retrying (use DLQ in production)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);                                 // 3 threads
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);   // manual commit
        factory.getContainerProperties().setPollTimeout(3_000);

        // Retry 2 times with 1-second gap, then log and skip (no infinite loop)
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(1_000L, 2))
        );

        return factory;
    }
    @Bean
    public NewTopic inventoryResponseTopic() {
        return TopicBuilder.name(inventoryResponseTopic)
                .partitions(3)
                .replicas(1)                              // set to 3 in a real cluster
                .config("retention.ms",    "604800000")  // 7 days
                .config("compression.type","snappy")
                .build();
    }
}
