package com.vcarrin87.dynamodb_example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.models.OrderLineItem;
import com.vcarrin87.dynamodb_example.repository.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemsServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemsService orderItemsService;

    @Test
    void createOrderItem_buildsKeysAndPersists() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderLineItem input = OrderLineItem.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(3)
                .price(BigDecimal.valueOf(19.99))
                .build();

        OrderLineItem saved = orderItemsService.createOrderItem(input);

        assertNotNull(saved.getOrderItemId());
        assertEquals(Keys.orderPk(orderId.toString()), saved.getPKey());
        assertEquals(EntityType.ORDER_ITEM.name(), saved.getEntityType());
        assertEquals(3, saved.getQuantity());
        verify(orderItemRepository).save(saved);
    }

    @Test
    void getAllOrderItems_returnsRepositoryItems() {
        List<OrderLineItem> expected = List.of(OrderLineItem.builder().orderItemId(UUID.randomUUID()).build());
        when(orderItemRepository.findAll()).thenReturn(expected);

        List<OrderLineItem> actual = orderItemsService.getAllOrderItems();

        assertEquals(expected, actual);
        verify(orderItemRepository).findAll();
    }

    @Test
    void getOrderItemsByOrderId_returnsRepositoryItems() {
        UUID orderId = UUID.randomUUID();
        List<OrderLineItem> expected = List.of(OrderLineItem.builder().orderId(orderId).build());
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(expected);

        List<OrderLineItem> actual = orderItemsService.getOrderItemsByOrderId(orderId);

        assertFalse(actual.isEmpty());
        assertEquals(orderId, actual.get(0).getOrderId());
        verify(orderItemRepository).findByOrderId(orderId);
    }
}
