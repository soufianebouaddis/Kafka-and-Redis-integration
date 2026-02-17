package os.org.inventory_service.model;

public enum ReservationStatus {
    /**
     * Stock held, order still in flight
     */
    RESERVED,
    /**
     * Order completed — stock moved to "sold" (availableQty already decremented)
     */
    CONFIRMED,
    /**
     * Order failed — stock returned to availableQuantity
     */
    RELEASED,
    /**
     * Reservation expired without resolution
     */
    EXPIRED
}
