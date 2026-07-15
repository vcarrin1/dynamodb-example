package com.vcarrin87.dynamodb_example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.repository.InventoryRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void createInventoryRecord_buildsKeysAndPersists() {
        UUID productId = UUID.randomUUID();
        InventoryItem input = InventoryItem.builder()
                .productId(productId)
                .stockLevel(42)
                .build();

        InventoryItem saved = inventoryService.createInventoryRecord(input);

        assertNotNull(saved);
        assertEquals(Keys.productPk(productId.toString()), saved.getPKey());
        assertEquals(Keys.inventorySk(), saved.getSKey());
        assertEquals(EntityType.INVENTORY.name(), saved.getEntityType());
        assertEquals(42, saved.getStockLevel());
        verify(inventoryRepository).save(saved);
    }

    @Test
    void getInventoryByProductId_delegatesToRepository() {
        UUID productId = UUID.randomUUID();
        InventoryItem expected = InventoryItem.builder().productId(productId).stockLevel(7).build();
        when(inventoryRepository.findByProductId(productId)).thenReturn(expected);

        InventoryItem actual = inventoryService.getInventoryByProductId(productId);

        assertEquals(expected, actual);
        verify(inventoryRepository).findByProductId(productId);
    }
}
