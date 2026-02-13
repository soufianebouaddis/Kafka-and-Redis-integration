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
public class PaymentEvent implements Serializable {
    private Long paymentId;
    private String orderNumber;
    private String customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    private PaymentEventType eventType;
    private PaymentStatus status;
    private LocalDateTime eventTimestamp;
    private String failureReason;
    
    public enum PaymentEventType {
        PAYMENT_REQUESTED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED
    }
    
    public enum PaymentStatus {
        PENDING,
        AUTHORIZED,
        CAPTURED,
        FAILED,
        REFUNDED,
        CANCELLED
    }
}
