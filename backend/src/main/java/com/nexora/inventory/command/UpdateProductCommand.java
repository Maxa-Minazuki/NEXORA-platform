package com.nexora.inventory.command;

import com.nexora.inventory.dto.InventoryDtos.ProductDto;
import com.nexora.inventory.dto.InventoryDtos.ProductRequest;
import com.nexora.inventory.service.InventoryService;
import com.nexora.shared.command.Command;

public final class UpdateProductCommand implements Command<ProductDto> {

    private final InventoryService receiver;
    private final Long productId;
    private final ProductRequest request;
    private final Long userId;

    public UpdateProductCommand(InventoryService receiver, Long productId, ProductRequest request, Long userId) {
        this.receiver = receiver;
        this.productId = productId;
        this.request = request;
        this.userId = userId;
    }

    @Override public String name() { return "UPDATE_PRODUCT"; }
    @Override public String entityType() { return "Product"; }
    @Override public ProductDto execute() { return receiver.update(productId, request, userId); }
    @Override public String entityId(ProductDto result) { return String.valueOf(productId); }
    @Override public String details() { return request.name() + ", ең аз " + request.minStock() + ", " + request.replenishmentStrategy(); }
}
