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

import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.service.InventoryService;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    @Test
    void createInventory_returnsServiceResult() {
        InventoryItem input = InventoryItem.builder().productId(UUID.randomUUID()).stockLevel(5).build();
        when(inventoryService.createInventoryRecord(input)).thenReturn(input);

        InventoryItem result = inventoryController.createInventory(input);

        assertEquals(input, result);
    }

    @Test
    void inventoryByProduct_delegatesToService() {
        UUID productId = UUID.randomUUID();
        InventoryItem expected = InventoryItem.builder().productId(productId).stockLevel(2).build();
        when(inventoryService.getInventoryByProductId(productId)).thenReturn(expected);

        InventoryItem actual = inventoryController.inventoryByProduct(productId);

        assertEquals(expected, actual);
        verify(inventoryService).getInventoryByProductId(productId);
    }
}
