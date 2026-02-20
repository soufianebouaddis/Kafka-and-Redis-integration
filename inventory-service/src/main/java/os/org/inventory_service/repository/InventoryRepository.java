package os.org.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import os.org.inventory_service.model.Inventory;
import os.org.inventory_service.model.InventoryStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(String productId);

    boolean existsByProductId(String productId);

    List<Inventory> findByStatus(InventoryStatus status);


    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.lowStockThreshold AND i.status = 'ACTIVE'")
    List<Inventory> findLowStockProducts();


    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity = 0 AND i.status = 'ACTIVE'")
    List<Inventory> findOutOfStockProducts();


    @Modifying
    @Query("""
        UPDATE Inventory i
        SET    i.availableQuantity = i.availableQuantity - :qty,
               i.reservedQuantity  = i.reservedQuantity  + :qty,
               i.updatedAt         = CURRENT_TIMESTAMP
        WHERE  i.productId          = :productId
          AND  i.availableQuantity  >= :qty
          AND  i.status             = 'ACTIVE'
        """)
    int reserveStock(@Param("productId") String productId, @Param("qty") int qty);

    @Modifying
    @Query("""
        UPDATE Inventory i
        SET    i.availableQuantity = i.availableQuantity + :qty,
               i.reservedQuantity  = i.reservedQuantity  - :qty,
               i.updatedAt         = CURRENT_TIMESTAMP
        WHERE  i.productId = :productId
          AND  i.reservedQuantity >= :qty
        """)
    int releaseStock(@Param("productId") String productId, @Param("qty") int qty);
}
