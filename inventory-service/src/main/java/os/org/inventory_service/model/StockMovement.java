package os.org.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


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

    @Column(nullable = false)
    private Integer quantityDelta;


    @Column(nullable = false)
    private Integer quantityAfter;

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
