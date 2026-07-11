package com.vcarrin87.dynamodb_example.graphql;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.service.OrderService;

@Controller
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates the GraphQL order controller.
     *
     * @param orderService order service
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates a new order.
     *
     * @param orderInput order payload
     * @return created order
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public OrderItem createOrder(@Argument("order") OrderItem orderInput) {
        return orderService.createOrder(orderInput);
    }

    /**
     * Deletes an order.
     *
     * @param orderId order UUID to delete
     * @param customerId customer UUID associated with order
     * @return confirmation message
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public String deleteOrder(@Argument UUID orderId, @Argument UUID customerId) {
        orderService.deleteOrder(orderId, customerId);
        return String.format("Order %s deleted successfully", orderId);
    }
}
