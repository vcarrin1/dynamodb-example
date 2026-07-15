package com.vcarrin87.dynamodb_example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.OrderItem;
import com.vcarrin87.dynamodb_example.repository.OrderRepository;
import com.vcarrin87.dynamodb_example.util.DynamoDbDeleteUtil;

import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DynamoDbDeleteUtil deleteUtil;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_persistsBuiltOrder() {
        OrderItem input = OrderItem.builder()
                .customerId(UUID.randomUUID().toString())
                .orderStatus("CREATED")
                .build();

        OrderItem saved = orderService.createOrder(input);

        assertEquals(EntityType.ORDER.name(), saved.getEntityType());
        assertEquals(input.getCustomerId(), saved.getCustomerId());
        assertTrue(saved.getSKey().startsWith("ORDER#"));
        verify(orderRepository).saveOrder(saved);
    }

    @Test
    void deleteOrder_whenMissing_throws() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(orderRepository.listOrdersForCustomer(customerId.toString())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> orderService.deleteOrder(orderId, customerId));
    }

    @Test
    void deleteOrder_whenPresent_executesAtomicDelete() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderItem existing = OrderItem.builder().orderId(orderId).customerId(customerId.toString()).build();
        when(orderRepository.listOrdersForCustomer(customerId.toString())).thenReturn(List.of(existing));
        when(deleteUtil.buildDeleteOperation(any(), any(), any())).thenReturn(TransactWriteItem.builder().build());

        orderService.deleteOrder(orderId, customerId);

        verify(deleteUtil).executeAtomicDeletes(any(), eq(String.format("Delete order %s", orderId)));
    }

    @Test
    void buildDeleteOperationsForCustomer_filtersNullIds() {
        UUID customerId = UUID.randomUUID();
        OrderItem valid = OrderItem.builder().orderId(UUID.randomUUID()).build();
        OrderItem invalid = OrderItem.builder().orderId(null).build();

        when(deleteUtil.buildDeleteOperation(any(), any(), any())).thenReturn(TransactWriteItem.builder().build());

        List<TransactWriteItem> operations = orderService.buildDeleteOperationsForCustomer(customerId, List.of(valid, invalid));

        assertEquals(1, operations.size());
    }
}
