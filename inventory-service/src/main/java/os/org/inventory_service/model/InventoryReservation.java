package os.org.inventory_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * InventoryReservation
 *
 * Created when an order reserves stock.
 * Released (status → RELEASED) when the order completes or fails.
 * This is the audit trail for every stock hold.
 */
@Entity
@Table(
        name = "inventory_reservations",
        indexes = {
                @Index(name = "idx_res_order_number", columnList = "orderNumber"),
                @Index(name = "idx_res_product_id",   columnList = "productId"),
                @Index(name = "idx_res_status",        columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to Inventory.productId (logical, not a JPA @ManyToOne to stay loosely coupled) */
    @Column(nullable = false, length = 50)
    private String productId;

    /** The order that created this reservation */
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    /** Quantity locked for this order */
    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(length = 300)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** When the reservation was released / confirmed */
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


}