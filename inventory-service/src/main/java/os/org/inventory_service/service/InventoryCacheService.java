package os.org.inventory_service.service;

import os.org.inventory_service.model.Inventory;

import java.util.Optional;

public interface InventoryCacheService {
    void seedStockCounter(String productId, long availableQty);
    Optional<Integer> getStockCounter(String productId);
    Long decrementStock(String productId, int qty);
    Long incrementStock(String productId, int qty);
    void evictStockCounter(String productId);
    void cacheInventory(Inventory inventory);
    Optional<Inventory> getCachedInventory(String productId);
    void evictInventory(String productId);
    void recordReservation(String orderNumber, int qty);
    Optional<Integer> getReservationQty(String orderNumber);
    void clearReservation(String orderNumber);
    boolean acquireLock(String productId, String lockValue);
    void releaseLock(String productId, String lockValue);
    String stockKey(String productId);

}
