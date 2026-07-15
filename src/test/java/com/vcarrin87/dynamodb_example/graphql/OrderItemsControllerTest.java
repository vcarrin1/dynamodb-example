package com.vcarrin87.dynamodb_example.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.OrderLineItem;
import com.vcarrin87.dynamodb_example.service.OrderItemsService;

@ExtendWith(MockitoExtension.class)
class OrderItemsControllerTest {

    @Mock
    private OrderItemsService orderItemsService;

    @InjectMocks
    private OrderItemsController orderItemsController;

    @Test
    void createOrderItem_returnsServiceResult() {
        OrderLineItem input = OrderLineItem.builder().orderId(UUID.randomUUID()).build();
        when(orderItemsService.createOrderItem(input)).thenReturn(input);

        OrderLineItem result = orderItemsController.createOrderItem(input);

        assertEquals(input, result);
    }

    @Test
    void orderItems_returnsAll() {
        List<OrderLineItem> expected = List.of(OrderLineItem.builder().orderItemId(UUID.randomUUID()).build());
        when(orderItemsService.getAllOrderItems()).thenReturn(expected);

        List<OrderLineItem> result = orderItemsController.orderItems();

        assertEquals(expected, result);
    }

    @Test
    void orderItemsByOrder_returnsFilteredItems() {
        UUID orderId = UUID.randomUUID();
        List<OrderLineItem> expected = List.of(OrderLineItem.builder().orderId(orderId).build());
        when(orderItemsService.getOrderItemsByOrderId(orderId)).thenReturn(expected);

        List<OrderLineItem> result = orderItemsController.orderItemsByOrder(orderId);

        assertEquals(expected, result);
        verify(orderItemsService).getOrderItemsByOrderId(orderId);
    }
}
