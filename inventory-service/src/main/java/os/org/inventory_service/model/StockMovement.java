package os.org.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * StockMovement
 *
 * Immutable ledger of every quantity change (IN / OUT / ADJUSTMENT).
 * Enables full audit trail and stock reconciliation.
 */
@Entity
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "idx_mv_product_id",   columnList = "productId"),
                @Index(name = "idx_mv_order_number",  columnList = "referenceId"),
                @Index(name = "idx_mv_type",          columnList = "movementType")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType movementType;

    /**
     * Positive = stock added (IN / RELEASE).
     * Negative = stock removed (OUT / RESERVE).
     */
    @Column(nullable = false)
    private Integer quantityDelta;

    /** Stock level after this movement */
    @Column(nullable = false)
    private Integer quantityAfter;

    /** orderNumber, refundId, manualAdjustmentId, etc. */
    @Column(length = 100)
    private String referenceId;

    @Column(length = 200)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


}
