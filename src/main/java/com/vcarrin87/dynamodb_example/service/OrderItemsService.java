package com.vcarrin87.dynamodb_example.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.OrderLineItem;
import com.vcarrin87.dynamodb_example.repository.OrderItemRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderItemsService {

    private final OrderItemRepository orderItemsRepository;

    /**
     * Creates the order items service.
     *
     * @param orderItemsRepository order items repository
     */
    public OrderItemsService(OrderItemRepository orderItemsRepository) {
        this.orderItemsRepository = orderItemsRepository;
    }

    /**
     * Creates a new order item.
     *
     * @param orderItemInput order item payload
     * @return persisted order item
     */
    public OrderLineItem createOrderItem(OrderLineItem orderItemInput) {
        UUID orderItemId = orderItemInput.getOrderItemId() != null
                ? orderItemInput.getOrderItemId()
                : UUID.randomUUID();

        OrderLineItem newOrderItem = OrderLineItem.builder()
                .pKey(Keys.orderPk(orderItemInput.getOrderId().toString()))
                .sKey(Keys.orderItemSk(orderItemId.toString()))
                .entityType(EntityType.ORDER_ITEM.name())
                .orderItemId(orderItemId)
                .orderId(orderItemInput.getOrderId())
                .productId(orderItemInput.getProductId())
                .quantity(orderItemInput.getQuantity())
                .price(orderItemInput.getPrice())
                .build();

        orderItemsRepository.save(newOrderItem);
        log.info("Order item created: {}", newOrderItem);
        return newOrderItem;
    }

    /**
     * Returns all order items.
     *
     * @return all order items
     */
    public List<OrderLineItem> getAllOrderItems() {
        List<OrderLineItem> allOrderItems = orderItemsRepository.findAll();
        log.info("Retrieved all order items: {}", allOrderItems);
        return allOrderItems;
    }

    /**
     * Returns order items by order ID.
     *
     * @param orderId order UUID
     * @return matching order items
     */
    public List<OrderLineItem> getOrderItemsByOrderId(UUID orderId) {
        List<OrderLineItem> orderItems = orderItemsRepository.findByOrderId(orderId);
        if (orderItems != null && !orderItems.isEmpty()) {
            log.info("Order items found for order ID {}: {}", orderId, orderItems);
        } else {
            log.warn("No order items found for order ID {}", orderId);
        }
        return orderItems;
    }
}
