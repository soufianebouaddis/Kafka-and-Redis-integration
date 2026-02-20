package os.org.inventory_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import os.org.common_service.events.InventoryEvent;
import os.org.inventory_service.service.InventoryKafkaProducer;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class InventoryKafkaProducerImpl implements InventoryKafkaProducer {


    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.inventory-response}")
    private String inventoryResponseTopic;

    public InventoryKafkaProducerImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishInventoryResponse(InventoryEvent event) {
        sendAsync(inventoryResponseTopic, event.getOrderNumber(), event);
    }

    private void sendAsync(String topic, String key, Object payload) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka SEND OK | topic={} key={} partition={} offset={}",
                            topic, key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    // In production → push to dead-letter queue / alert
                    log.error("Kafka SEND FAILED | topic={} key={} error={}",
                            topic, key, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Kafka send exception | topic={} key={} error={}", topic, key, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to Kafka", e);
        }
    }

}
