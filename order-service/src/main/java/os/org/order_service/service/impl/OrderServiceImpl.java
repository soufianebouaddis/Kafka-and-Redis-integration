package os.org.order_service.service.impl;

import org.springframework.stereotype.Service;
import os.org.order_service.model.Order;
import os.org.order_service.repository.OrderRepository;
import os.org.order_service.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    private OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository){
        this.orderRepository = orderRepository;
    }


    @Override
    public void createOrder(Object order) {
        
    }

    @Override
    public Object getOrderById(String orderId) {
        return null;
    }

    @Override
    public void updateOrder(Object order) {

    }

    @Override
    public void deleteOrder(String orderId) {

    }
}
