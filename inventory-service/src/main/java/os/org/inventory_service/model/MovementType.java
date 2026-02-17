package os.org.inventory_service.model;

public enum MovementType {
    /** New stock received from supplier */
    STOCK_IN,
    /** Stock reserved for an order */
    RESERVATION,
    /** Reservation released (order cancelled / failed) */
    RELEASE,
    /** Order shipped — removes reserved qty permanently */
    SALE,
    /** Manual correction */
    ADJUSTMENT
}
