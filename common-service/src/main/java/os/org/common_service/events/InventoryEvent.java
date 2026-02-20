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
    private long inventoryId;
    private String productId;
    private String orderNumber;
    private long quantity;
    private long availableQuantity;
    private long quantityRequested;
    private EventType eventType;
    private long availableAfter;
    private LocalDateTime eventTimestamp;
    private String failureReason;
    

}