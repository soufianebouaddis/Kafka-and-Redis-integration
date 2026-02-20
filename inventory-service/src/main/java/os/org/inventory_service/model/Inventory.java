package os.org.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Inventory
 * One row per product.  Tracks total, available, and reserved quantities.
 */
@Entity
@Table(
        name = "inventory",
        indexes = {
                @Index(name = "idx_product_id", columnList = "productId", unique = true),
                @Index(name = "idx_status",     columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private long totalQuantity;

    @Column(nullable = false)
    private long availableQuantity;

    @Column(nullable = false)
    private long reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status;

    @Column(nullable = false)
    @Builder.Default
    private long lowStockThreshold = 10;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = InventoryStatus.ACTIVE;
        if (reservedQuantity == 0) reservedQuantity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }



    public boolean hasStock(int qty) {
        return availableQuantity != 0 && availableQuantity >= qty;
    }

    public boolean isLowStock() {
        return availableQuantity != 0 && availableQuantity <= lowStockThreshold;
    }


}
