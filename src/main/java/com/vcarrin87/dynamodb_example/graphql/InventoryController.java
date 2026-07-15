package com.vcarrin87.dynamodb_example.graphql;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.vcarrin87.dynamodb_example.models.InventoryItem;
import com.vcarrin87.dynamodb_example.service.InventoryService;

@Controller
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Creates the GraphQL inventory controller.
     *
     * @param inventoryService inventory service
     */
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Creates a new inventory record.
     *
     * @param inventory inventory payload
     * @return persisted inventory item
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @MutationMapping
    public InventoryItem createInventory(@Argument("inventory") InventoryItem inventory) {
        return inventoryService.createInventoryRecord(inventory);
    }

    /**
     * Returns inventory details by product ID.
     *
     * @param productId product UUID
     * @return inventory record or null when not found
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")
    @QueryMapping
    public InventoryItem inventoryByProduct(@Argument UUID productId) {
        return inventoryService.getInventoryByProductId(productId);
    }
}
