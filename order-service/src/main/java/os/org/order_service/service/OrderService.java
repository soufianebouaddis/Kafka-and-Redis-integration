package os.org.order_service.service;

public interface OrderService<T> {
    void createOrder(T order);
    T getOrderById(String orderId);
    void updateOrder(T order);
    void deleteOrder(String orderId);
}
