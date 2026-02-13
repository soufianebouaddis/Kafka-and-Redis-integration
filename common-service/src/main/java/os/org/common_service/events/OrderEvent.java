package os.org.common_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime eventTimestamp;
    private String eventType; // CREATED, VALIDATED, INVENTORY_RESERVED, PAYMENT_COMPLETED, etc.
    private String failureReason;
    
    public enum OrderStatus {
        PENDING,
        INVENTORY_CHECKING,
        INVENTORY_RESERVED,
        INVENTORY_FAILED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        COMPLETED,
        CANCELLED,
        FAILED
    }
}
