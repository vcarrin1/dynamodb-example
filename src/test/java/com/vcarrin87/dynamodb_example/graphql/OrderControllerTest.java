package com.vcarrin87.dynamodb_example.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void createOrder_returnsServiceResult() {
        OrderItem input = OrderItem.builder().customerId(UUID.randomUUID().toString()).build();
        OrderItem saved = OrderItem.builder().orderId(UUID.randomUUID()).build();
        when(orderService.createOrder(input)).thenReturn(saved);

        OrderItem result = orderController.createOrder(input);

        assertEquals(saved, result);
    }

    @Test
    void deleteOrder_callsServiceAndReturnsMessage() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        String result = orderController.deleteOrder(orderId, customerId);

        verify(orderService).deleteOrder(orderId, customerId);
        assertEquals(String.format("Order %s deleted successfully", orderId), result);
    }
}
