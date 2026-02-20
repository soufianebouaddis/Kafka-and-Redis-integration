package os.org.inventory_service.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import os.org.common_service.events.InventoryEvent;
import os.org.common_service.events.OrderEvent;
import os.org.inventory_service.model.*;
import os.org.inventory_service.repository.InventoryRepository;
import os.org.inventory_service.repository.InventoryReservationRepository;
import os.org.inventory_service.repository.StockMovementRepository;
import os.org.inventory_service.service.InventoryCacheService;
import os.org.inventory_service.service.InventoryKafkaProducer;
import os.org.inventory_service.service.InventoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {


    private final InventoryRepository inventoryRepo;
    private final InventoryReservationRepository reservationRepo;
    private final StockMovementRepository movementRepo;
    private final InventoryCacheService cache;
    private final InventoryKafkaProducer producer;

    public InventoryServiceImpl(InventoryRepository inventoryRepo,
                                InventoryReservationRepository reservationRepo,
                                StockMovementRepository movementRepo,
                                InventoryCacheService cache,
                                InventoryKafkaProducer producer) {
        this.inventoryRepo = inventoryRepo;
        this.reservationRepo = reservationRepo;
        this.movementRepo = movementRepo;
        this.cache = cache;
        this.producer = producer;
    }


    @Override
    public InventoryEvent reserveStock(OrderEvent order) {
        String product = order.getProductId();
        String orderNumber = order.getOrderNumber();
        int qty = order.getQuantity();
        String lockValue = UUID.randomUUID().toString();

        log.info("RESERVE START | order={} product={} qty={}", orderNumber, product, qty);


        if (reservationRepo.existsByOrderNumber(orderNumber)) {
            log.warn("Duplicate reservation detected | order={}", orderNumber);
            return buildEvent(order, InventoryEvent.EventType.RESERVATION_CONFIRMED,
                    0, "Duplicate — already reserved");
        }


        if (!cache.acquireLock(product, lockValue)) {
            log.warn("Could not acquire lock for product={}", product);
            return publishAndReturn(buildEvent(order, InventoryEvent.EventType.RESERVATION_FAILED,
                    0, "System busy — retry"));
        }

        try {
            // Seed counter from DB if not yet in Redis
            if (cache.getStockCounter(product).isEmpty()) {
                long dbQty = inventoryRepo.findByProductId(product)
                        .map(Inventory::getAvailableQuantity)
                        .orElseThrow(() -> new RuntimeException("Product not found: " + product));
                cache.seedStockCounter(product, dbQty);
            }

            Long afterDecr = cache.decrementStock(product, qty);

            if (afterDecr == null) {
                // Redis error — fall through to DB-only path
                log.warn("Redis DECR returned null, falling to DB path | product={}", product);
                return dbReserve(order, lockValue);
            }

            if (afterDecr < 0) {
                // Not enough stock — rollback Redis counter immediately
                cache.incrementStock(product, qty);
                log.warn("INSUFFICIENT STOCK (Redis) | product={} available={}", product, afterDecr + qty);
                return publishAndReturn(buildEvent(order, InventoryEvent.EventType.RESERVATION_FAILED,
                        0, "Insufficient stock"));
            }


            int updated = inventoryRepo.reserveStock(product, qty);
            if (updated == 0) {
                // DB check failed (race condition / drift) — rollback Redis
                cache.incrementStock(product, qty);
                log.error("DB reserveStock returned 0 | product={} (race condition?)", product);
                return publishAndReturn(buildEvent(order, InventoryEvent.EventType.RESERVATION_FAILED, 0, "Concurrent stock update — insufficient stock"));
            }


            InventoryReservation reservation = InventoryReservation.builder()
                    .productId(product)
                    .orderNumber(orderNumber)
                    .quantity(qty)
                    .status(ReservationStatus.RESERVED)
                    .build();
            reservationRepo.save(reservation);

            Inventory inv = inventoryRepo.findByProductId(product).orElseThrow();
            journalMovement(product, MovementType.RESERVATION,-qty, inv.getAvailableQuantity(), orderNumber, null);


            cache.cacheInventory(inv);
            cache.recordReservation(orderNumber, qty);


            if (inv.isLowStock()) {
                log.warn("LOW STOCK ALERT | product={} available={}", product, inv.getAvailableQuantity());
            }

            log.info("RESERVE OK | order={} product={} remainingAvailable={}",
                    orderNumber, product, afterDecr);

            InventoryEvent event = buildEvent(order, InventoryEvent.EventType.RESERVATION_CONFIRMED,
                    (int) (long) afterDecr, null);
            return publishAndReturn(event);

        } finally {
            cache.releaseLock(product, lockValue);
        }
    }

    @Override
    public void releaseStock(OrderEvent order) {
        String product = order.getProductId();
        String orderNumber = order.getOrderNumber();

        log.info("RELEASE START | order={} product={}", orderNumber, product);

        // Find reservation
        Optional<InventoryReservation> resOpt = reservationRepo.findByOrderNumber(orderNumber);
        if (resOpt.isEmpty()) {
            log.warn("No reservation found for order={} — nothing to release", orderNumber);
            return;
        }

        InventoryReservation reservation = resOpt.get();
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Reservation already resolved | order={} status={}", orderNumber, reservation.getStatus());
            return;
        }

        int qty = reservation.getQuantity();

        // DB update (atomic)
        inventoryRepo.releaseStock(product, qty);

        // Update reservation record
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setResolvedAt(LocalDateTime.now());
        reservationRepo.save(reservation);

        // Redis: increment counter back
        cache.incrementStock(product, qty);
        cache.clearReservation(orderNumber);

        // Evict stale POJO so next REST read fetches fresh data
        cache.evictInventory(product);

        // Journal
        Inventory inv = inventoryRepo.findByProductId(product).orElseThrow();
        journalMovement(product, MovementType.RELEASE,
                qty, inv.getAvailableQuantity(), orderNumber, "Order failed — stock released");

        // Re-cache updated POJO
        cache.cacheInventory(inv);

        // Publish release event (informational — Order Service may ignore)
        InventoryEvent event = buildEvent(order, InventoryEvent.EventType.RESERVATION_RELEASED,
                inv.getAvailableQuantity(), null);
        producer.publishInventoryResponse(event);

        log.info("RELEASE OK | order={} product={} qty={} available={}",
                orderNumber, product, qty, inv.getAvailableQuantity());
    }

    @Override
    public Inventory createInventory(Inventory inventory) {
        if (inventoryRepo.existsByProductId(inventory.getProductId())) {
            throw new IllegalStateException("Product already exists: " + inventory.getProductId());
        }
        inventory.setReservedQuantity(0);
        inventory.setStatus(InventoryStatus.ACTIVE);
        Inventory saved = inventoryRepo.save(inventory);

        // Seed Redis counter
        cache.seedStockCounter(saved.getProductId(), saved.getAvailableQuantity());
        cache.cacheInventory(saved);

        journalMovement(saved.getProductId(), MovementType.STOCK_IN, saved.getTotalQuantity(), saved.getAvailableQuantity(), null, "Initial stock");

        log.info("Inventory created | product={} qty={}", saved.getProductId(), saved.getTotalQuantity());
        return saved;
    }

    @Override
    public Optional<Inventory> getByProductId(String productId) {
        Optional<Inventory> cached = cache.getCachedInventory(productId);
        if (cached.isPresent()) return cached;

        Optional<Inventory> fromDb = inventoryRepo.findByProductId(productId);
        fromDb.ifPresent(inv -> {
            cache.cacheInventory(inv);
            cache.seedStockCounter(productId, inv.getAvailableQuantity());
        });
        return fromDb;
    }

    @Override
    public List<Inventory> getAll() {
        return inventoryRepo.findAll();
    }

    @Override
    public List<Inventory> getLowStockProducts() {
        return inventoryRepo.findLowStockProducts();
    }

    @Override
    public Inventory addStock(String productId, int qty, String notes) {
        Inventory inv = inventoryRepo.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        inv.setTotalQuantity(inv.getTotalQuantity() + qty);
        inv.setAvailableQuantity(inv.getAvailableQuantity() + qty);
        inv = inventoryRepo.save(inv);

        cache.incrementStock(productId, qty);
        cache.cacheInventory(inv);

        journalMovement(productId, MovementType.STOCK_IN,
                qty, inv.getAvailableQuantity(), null, notes);

        log.info("Stock added | product={} qty={} newAvailable={}", productId, qty, inv.getAvailableQuantity());
        return inv;
    }

    @Override
    public InventoryEvent dbReserve(OrderEvent order, String lockValue) {
        try {
            int updated = inventoryRepo.reserveStock(order.getProductId(), order.getQuantity());
            if (updated == 0) {
                return publishAndReturn(buildEvent(order, InventoryEvent.EventType.RESERVATION_FAILED,
                        0, "Insufficient stock (DB fallback)"));
            }

            InventoryReservation reservation = InventoryReservation.builder()
                    .productId(order.getProductId())
                    .orderNumber(order.getOrderNumber())
                    .quantity(order.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .build();
            reservationRepo.save(reservation);

            Inventory inv = inventoryRepo.findByProductId(order.getProductId()).orElseThrow();
            cache.cacheInventory(inv);
            cache.recordReservation(order.getOrderNumber(), order.getQuantity());

            return publishAndReturn(buildEvent(order, InventoryEvent.EventType.RESERVATION_CONFIRMED, inv.getAvailableQuantity(), null));
        } finally {
            cache.releaseLock(order.getProductId(), lockValue);
        }
    }

    @Override
    public void journalMovement(String productId, MovementType type, long delta, long after, String refId, String notes) {
        movementRepo.save(StockMovement.builder()
                .productId(productId)
                .movementType(type)
                .quantityDelta(delta)
                .quantityAfter(after)
                .referenceId(refId)
                .notes(notes)
                .build());
    }

    @Override
    public InventoryEvent buildEvent(OrderEvent order, InventoryEvent.EventType type, long availableAfter, String failure) {
        return InventoryEvent.builder()
                .productId(order.getProductId())
                .orderNumber(order.getOrderNumber())
                .quantityRequested(order.getQuantity())
                .availableAfter(availableAfter)
                .eventType(type)
                .eventTimestamp(LocalDateTime.now())
                .failureReason(failure)
                .build();
    }
    @Override
    public InventoryEvent publishAndReturn(InventoryEvent event) {
        producer.publishInventoryResponse(event);
        return event;
    }
}



