package com.nexora.inventory.command;

import com.nexora.inventory.dto.InventoryDtos.ProductDto;
import com.nexora.inventory.dto.InventoryDtos.ProductRequest;
import com.nexora.inventory.service.InventoryService;
import com.nexora.shared.command.Command;

public final class CreateProductCommand implements Command<ProductDto> {

    private final InventoryService receiver;
    private final ProductRequest request;
    private final Long userId;

    public CreateProductCommand(InventoryService receiver, ProductRequest request, Long userId) {
        this.receiver = receiver;
        this.request = request;
        this.userId = userId;
    }

    @Override public String name() { return "CREATE_PRODUCT"; }
    @Override public String entityType() { return "Product"; }
    @Override public ProductDto execute() { return receiver.create(request, userId); }
    @Override public String entityId(ProductDto result) { return result == null ? null : String.valueOf(result.id()); }
    @Override public String details() { return request.sku() + " " + request.name() + ", мөлшері " + request.quantity(); }
}
