package com.vcarrin87.dynamodb_example.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vcarrin87.dynamodb_example.models.EntityType;
import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.models.Keys;
import com.vcarrin87.dynamodb_example.repository.InventoryRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * Creates the inventory service.
     *
     * @param inventoryRepository inventory repository
     */
    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Creates a new inventory record.
     *
     * @param inventoryInput inventory payload
     * @return persisted inventory item
     */
    public InventoryItem createInventoryRecord(InventoryItem inventoryInput) {
        InventoryItem inventory = InventoryItem.builder()
                .pKey(Keys.productPk(inventoryInput.getProductId().toString()))
                .sKey(Keys.inventorySk())
                .entityType(EntityType.INVENTORY.name())
                .productId(inventoryInput.getProductId())
                .stockLevel(inventoryInput.getStockLevel())
                .build();

        inventoryRepository.save(inventory);
        log.info("Inventory record created: {}", inventory);
        return inventory;
    }

    /**
     * Gets inventory by product ID.
     *
     * @param productId product UUID
     * @return inventory item, or null when not found
     */
    public InventoryItem getInventoryByProductId(UUID productId) {
        InventoryItem inventory = inventoryRepository.findByProductId(productId);
        if (inventory != null) {
            log.info("Inventory found for product ID {}: {}", productId, inventory);
        } else {
            log.warn("No inventory found for product ID {}", productId);
        }
        return inventory;
    }
}
