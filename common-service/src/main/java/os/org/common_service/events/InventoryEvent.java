package os.org.common_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent implements Serializable {
    private Long inventoryId;
    private String productId;
    private String orderNumber;
    private Integer quantity;
    private Integer availableQuantity;
    private InventoryEventType eventType;
    private LocalDateTime eventTimestamp;
    private String failureReason;
    
    public enum InventoryEventType {
        RESERVATION_REQUESTED,
        RESERVATION_CONFIRMED,
        RESERVATION_FAILED,
        RESERVATION_RELEASED,
        STOCK_UPDATED,
        LOW_STOCK_ALERT
    }
}