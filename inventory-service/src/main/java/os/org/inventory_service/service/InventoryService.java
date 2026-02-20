package os.org.inventory_service.service;

import os.org.common_service.events.InventoryEvent;
import os.org.common_service.events.OrderEvent;
import os.org.inventory_service.model.Inventory;
import os.org.inventory_service.model.MovementType;

import java.util.List;
import java.util.Optional;

public interface InventoryService {
    InventoryEvent reserveStock(OrderEvent order);
    void releaseStock(OrderEvent order);
    Inventory createInventory(Inventory inventory);
    Optional<Inventory> getByProductId(String productId);
    List<Inventory> getAll();
    List<Inventory> getLowStockProducts();
    Inventory addStock(String productId, int qty, String notes);
    InventoryEvent dbReserve(OrderEvent order, String lockValue);
    void journalMovement(String productId, MovementType type,
                         int delta, int after, String refId, String notes);
    InventoryEvent buildEvent(OrderEvent order, InventoryEvent.EventType type,
                             Integer availableAfter, String failure);
    InventoryEvent publishAndReturn(InventoryEvent event);
}
