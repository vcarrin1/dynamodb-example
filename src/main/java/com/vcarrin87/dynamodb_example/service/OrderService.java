package com.vcarrin87.dynamodb_example.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * Creates the order service.
     *
     * @param orderRepository order repository
     */
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Returns all orders for a customer.
     *
     * @param customerId customer UUID
     * @return matching order list
     */
    public List<OrderItem> listOrdersForCustomer(UUID customerId) {
        return orderRepository.listOrdersForCustomer(customerId.toString());
    }

    /**
     * Creates a new order record.
     *
     * @param orderInput order payload
     * @return persisted order
     */
    public OrderItem createOrder(OrderItem orderInput) {
        UUID orderId = orderInput.getOrderId() != null ? orderInput.getOrderId() : UUID.randomUUID();
        OrderItem newOrder = OrderItem.builder()
                .pKey(Keys.customerPk(orderInput.getCustomerId()))
            .sKey(Keys.customerOrderSk(orderId.toString()))
                .entityType(EntityType.ORDER.name())
                .orderId(orderId)
                .customerId(orderInput.getCustomerId())
                .orderStatus(orderInput.getOrderStatus())
                .deliveryDate(orderInput.getDeliveryDate())
                .createdAt(Instant.now())
                .build();

        orderRepository.saveOrder(newOrder);
        return newOrder;
    }
}

