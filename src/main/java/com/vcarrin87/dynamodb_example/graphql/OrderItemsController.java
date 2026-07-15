package com.vcarrin87.dynamodb_example.graphql;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.vcarrin87.dynamodb_example.models.OrderLineItem;
import com.vcarrin87.dynamodb_example.service.OrderItemsService;

@Controller
public class OrderItemsController {

    private final OrderItemsService orderItemsService;

    /**
     * Creates the GraphQL order items controller.
     *
     * @param orderItemsService order items service
     */
    public OrderItemsController(OrderItemsService orderItemsService) {
        this.orderItemsService = orderItemsService;
    }

    /**
     * Creates a new order item.
     *
     * @param orderItem order item payload
     * @return persisted order item
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public OrderLineItem createOrderItem(@Argument("orderItem") OrderLineItem orderItem) {
        return orderItemsService.createOrderItem(orderItem);
    }

    /**
     * Returns all order items.
     *
     * @return all order items
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public List<OrderLineItem> orderItems() {
        return orderItemsService.getAllOrderItems();
    }

    /**
     * Returns order items by order ID.
     *
     * @param orderId order UUID
     * @return matching order items
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public List<OrderLineItem> orderItemsByOrder(@Argument UUID orderId) {
        return orderItemsService.getOrderItemsByOrderId(orderId);
    }
}
