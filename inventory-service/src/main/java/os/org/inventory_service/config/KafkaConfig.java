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

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.inventory-response}")
    private String inventoryResponseTopic;


    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,            bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,         StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,       JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG,                         "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,           true);
        props.put(ProducerConfig.RETRIES_CONFIG,                      Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,             "snappy");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,                   16_384);
        props.put(ProducerConfig.LINGER_MS_CONFIG,                    10);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG,                33_554_432);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,           30_000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,          120_000);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,             bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,        StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,      JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,                   "*");
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS,              false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                      "inventory-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,             "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,            false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,              500);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,          300_000);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,               1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,             500);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,               "read_committed");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,            10_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,         3_000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setPollTimeout(3_000);
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(1_000L, 2))
        );

        return factory;
    }
    @Bean
    public NewTopic inventoryResponseTopic() {
        return TopicBuilder.name(inventoryResponseTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms",    "604800000")
                .config("compression.type","snappy")
                .build();
    }
}
