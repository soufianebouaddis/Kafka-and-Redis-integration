package os.org.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import os.org.inventory_service.model.MovementType;
import os.org.inventory_service.model.StockMovement;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId);

    List<StockMovement> findByReferenceId(String referenceId);

    List<StockMovement> findByMovementType(MovementType movementType);
}
