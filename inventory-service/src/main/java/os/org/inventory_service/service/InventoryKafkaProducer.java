package os.org.inventory_service.service;

import os.org.common_service.events.InventoryEvent;

public interface InventoryKafkaProducer {
    void publishInventoryResponse(InventoryEvent event);

}
