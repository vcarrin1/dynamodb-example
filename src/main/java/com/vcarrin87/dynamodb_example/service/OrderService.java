package com.vcarrin87.dynamodb_example.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.repository.OrderRepository;
import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final DynamoDbDeleteUtil deleteUtil;

    /**
     * Creates the order service.
     *
     * @param orderRepository order repository
     * @param deleteUtil DynamoDB delete utility for transactions
     */
    public OrderService(OrderRepository orderRepository, DynamoDbDeleteUtil deleteUtil) {
        this.orderRepository = orderRepository;
        this.deleteUtil = deleteUtil;
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

    /**
     * Deletes an order by ID.
     *
     * @param orderId order UUID to delete
     * @param customerId customer UUID associated with order
     * @throws RuntimeException if deletion fails
     */
    public void deleteOrder(UUID orderId, UUID customerId) {
        // Verify order exists
        List<OrderItem> orders = listOrdersForCustomer(customerId);
        boolean orderExists = orders.stream()
            .anyMatch(o -> o.getOrderId().equals(orderId));
        
        if (!orderExists) {
            log.warn("No order found with ID {}", orderId);
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        // Build and execute atomic delete
        var deleteOp = deleteUtil.buildDeleteOperation(
            "OrderTable",
            Keys.customerPk(customerId.toString()),
            Keys.customerOrderSk(orderId.toString())
        );
        
        deleteUtil.executeAtomicDeletes(
            java.util.List.of(deleteOp),
            String.format("Delete order %s", orderId)
        );
        
        log.info("Order with ID {} deleted", orderId);
    }

    /**
     * Builds delete operations for all orders belonging to a customer.
     *
     * @param customerId customer UUID
     * @param orders customer orders to delete
     * @return transactional delete operations
     */
    public List<TransactWriteItem> buildDeleteOperationsForCustomer(UUID customerId, List<OrderItem> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        return orders.stream()
            .map(OrderItem::getOrderId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .map(orderId -> deleteUtil.buildDeleteOperation(
                "OrderTable",
                Keys.customerPk(customerId.toString()),
                Keys.customerOrderSk(orderId.toString())
            ))
            .toList();
    }
}

