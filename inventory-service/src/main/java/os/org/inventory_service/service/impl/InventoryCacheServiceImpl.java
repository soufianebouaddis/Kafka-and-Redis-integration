package os.org.inventory_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import os.org.inventory_service.model.Inventory;
import os.org.inventory_service.service.InventoryCacheService;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class InventoryCacheServiceImpl implements InventoryCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    @Value("${app.redis.inventory-prefix}")
    private String inventoryPrefix;          // "inventory:"

    @Value("${app.redis.reservation-prefix}")
    private String reservationPrefix;        // "reservation:"

    @Value("${app.redis.lock-prefix}")
    private String lockPrefix;               // "lock:inventory:"

    @Value("${app.redis.stock-ttl}")
    private long stockTtl;                   // seconds

    @Value("${app.redis.reservation-ttl}")
    private long reservationTtl;

    @Value("${app.redis.lock-ttl}")
    private long lockTtl;

    public InventoryCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                 RedisTemplate<String, String> stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public void seedStockCounter(String productId, int availableQty) {
        String key = stockKey(productId);
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, String.valueOf(availableQty), Duration.ofSeconds(stockTtl));
            log.debug("Seeded stock counter | product={} qty={}", productId, availableQty);
        } catch (Exception e) {
            log.warn("Could not seed stock counter | product={} error={}", productId, e.getMessage());
        }
    }

    @Override
    public Optional<Integer> getStockCounter(String productId) {
        try {
            String val = stringRedisTemplate.opsForValue().get(stockKey(productId));
            if (val == null) return Optional.empty();
            return Optional.of(Integer.parseInt(val));
        } catch (Exception e) {
            log.warn("getStockCounter error | product={} error={}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Long decrementStock(String productId, int qty) {
        try {
            Long after = stringRedisTemplate.opsForValue().decrement(stockKey(productId), qty);
            log.debug("DECR stock | product={} qty={} after={}", productId, qty, after);
            return after;
        } catch (Exception e) {
            log.error("decrementStock error | product={} error={}", productId, e.getMessage());
            return null;
        }
    }

    @Override
    public Long incrementStock(String productId, int qty) {
        try {
            Long after = stringRedisTemplate.opsForValue().increment(stockKey(productId), qty);
            log.debug("INCR stock | product={} qty={} after={}", productId, qty, after);
            return after;
        } catch (Exception e) {
            log.error("incrementStock error | product={} error={}", productId, e.getMessage());
            return null;
        }
    }

    @Override
    public void evictStockCounter(String productId) {
        try {
            stringRedisTemplate.delete(stockKey(productId));
            log.debug("Evicted stock counter | product={}", productId);
        } catch (Exception e) {
            log.warn("evictStockCounter error | product={} error={}", productId, e.getMessage());
        }
    }

    @Override
    public void cacheInventory(Inventory inventory) {
        try {
            String key = inventoryPrefix + inventory.getProductId();
            redisTemplate.opsForValue()
                    .set(key, inventory, Duration.ofSeconds(stockTtl));
            log.debug("Cached Inventory POJO | product={}", inventory.getProductId());
        } catch (Exception e) {
            log.warn("cacheInventory error | product={} error={}", inventory.getProductId(), e.getMessage());
        }
    }

    @Override
    public Optional<Inventory> getCachedInventory(String productId) {
        try {
            Object val = redisTemplate.opsForValue().get(inventoryPrefix + productId);
            if (val instanceof Inventory inv) {
                log.debug("Cache HIT Inventory POJO | product={}", productId);
                return Optional.of(inv);
            }
        } catch (Exception e) {
            log.warn("getCachedInventory error | product={} error={}", productId, e.getMessage());
        }
        log.debug("Cache MISS Inventory POJO | product={}", productId);
        return Optional.empty();
    }

    @Override
    public void evictInventory(String productId) {
        try {
            redisTemplate.delete(inventoryPrefix + productId);
        } catch (Exception e) {
            log.warn("evictInventory error | product={} error={}", productId, e.getMessage());
        }
    }

    @Override
    public void recordReservation(String orderNumber, int qty) {
        try {
            String key = reservationPrefix + orderNumber;
            stringRedisTemplate.opsForValue()
                    .set(key, String.valueOf(qty), Duration.ofSeconds(reservationTtl));
            log.debug("Recorded reservation | order={} qty={}", orderNumber, qty);
        } catch (Exception e) {
            log.warn("recordReservation error | order={} error={}", orderNumber, e.getMessage());
        }
    }

    @Override
    public Optional<Integer> getReservationQty(String orderNumber) {
        try {
            String val = stringRedisTemplate.opsForValue().get(reservationPrefix + orderNumber);
            if (val == null) return Optional.empty();
            return Optional.of(Integer.parseInt(val));
        } catch (Exception e) {
            log.warn("getReservationQty error | order={} error={}", orderNumber, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void clearReservation(String orderNumber) {
        try {
            stringRedisTemplate.delete(reservationPrefix + orderNumber);
        } catch (Exception e) {
            log.warn("clearReservation error | order={} error={}", orderNumber, e.getMessage());
        }
    }

    @Override
    public boolean acquireLock(String productId, String lockValue) {
        try {
            String key = lockPrefix + productId;
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, lockValue, Duration.ofSeconds(lockTtl));
            boolean acquired = Boolean.TRUE.equals(ok);
            log.debug("acquireLock | product={} acquired={}", productId, acquired);
            return acquired;
        } catch (Exception e) {
            log.error("acquireLock error | product={} error={}", productId, e.getMessage());
            return false;
        }
    }

    @Override
    public void releaseLock(String productId, String lockValue) {
        try {
            String key = lockPrefix + productId;
            String current = stringRedisTemplate.opsForValue().get(key);
            if (lockValue.equals(current)) {
                stringRedisTemplate.delete(key);
                log.debug("releaseLock | product={}", productId);
            } else {
                log.warn("releaseLock: lock already expired or stolen | product={}", productId);
            }
        } catch (Exception e) {
            log.error("releaseLock error | product={} error={}", productId, e.getMessage());
        }
    }

    @Override
    public String stockKey(String productId) {
        return inventoryPrefix + productId;
    }
}
