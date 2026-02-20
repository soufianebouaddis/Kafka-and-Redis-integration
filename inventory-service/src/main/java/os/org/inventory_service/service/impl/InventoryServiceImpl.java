package os.org.inventory_service.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import os.org.common_service.events.InventoryEvent;
import os.org.common_service.events.OrderEvent;
import os.org.inventory_service.model.Inventory;
import os.org.inventory_service.model.MovementType;
import os.org.inventory_service.service.InventoryService;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Override
    public InventoryEvent reserveStock(OrderEvent order) {
        return null;
    }

    @Override
    public void releaseStock(OrderEvent order) {

    }

    @Override
    public Inventory createInventory(Inventory inventory) {
        return null;
    }

    @Override
    public Optional<Inventory> getByProductId(String productId) {
        return Optional.empty();
    }

    @Override
    public List<Inventory> getAll() {
        return List.of();
    }

    @Override
    public List<Inventory> getLowStockProducts() {
        return List.of();
    }

    @Override
    public Inventory addStock(String productId, int qty, String notes) {
        return null;
    }

    @Override
    public InventoryEvent dbReserve(OrderEvent order, String lockValue) {
        return null;
    }

    @Override
    public void journalMovement(String productId, MovementType type, int delta, int after, String refId, String notes) {

    }

    @Override
    public InventoryEvent buildEvent(OrderEvent order, InventoryEvent.EventType type, Integer availableAfter, String failure) {
        return null;
    }

    @Override
    public InventoryEvent publishAndReturn(InventoryEvent event) {
        return null;
    }
}
