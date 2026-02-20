package os.org.inventory_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import os.org.inventory_service.model.InventoryReservation;
import os.org.inventory_service.model.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<InventoryReservation> findByProductId(String productId);

    List<InventoryReservation> findByStatus(ReservationStatus status);

    /** Find reservations still RESERVED but created before a threshold (for expiry jobs) */
    @Query("""
        SELECT r FROM InventoryReservation r
        WHERE  r.status = 'RESERVED'
          AND  r.createdAt < :threshold
        """)
    List<InventoryReservation> findExpiredReservations(@Param("threshold") LocalDateTime threshold);

    /** Bulk-expire stale reservations */
    @Modifying
    @Query("""
        UPDATE InventoryReservation r
        SET    r.status     = 'EXPIRED',
               r.resolvedAt = CURRENT_TIMESTAMP
        WHERE  r.status    = 'RESERVED'
          AND  r.createdAt < :threshold
        """)
    int expireStaleReservations(@Param("threshold") LocalDateTime threshold);
}
