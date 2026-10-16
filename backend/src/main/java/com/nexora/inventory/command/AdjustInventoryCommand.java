package com.nexora.inventory.command;

import com.nexora.inventory.dto.InventoryDtos.AdjustStockRequest;
import com.nexora.inventory.dto.InventoryDtos.AdjustmentResult;
import com.nexora.inventory.service.InventoryService;
import com.nexora.shared.command.Command;

/** Command: a stock receipt, write-off or correction requested by a user. */
public final class AdjustInventoryCommand implements Command<AdjustmentResult> {

    private final InventoryService receiver;
    private final Long productId;
    private final AdjustStockRequest request;
    private final Long userId;

    public AdjustInventoryCommand(InventoryService receiver, Long productId, AdjustStockRequest request, Long userId) {
        this.receiver = receiver;
        this.productId = productId;
        this.request = request;
        this.userId = userId;
    }

    @Override public String name() { return "ADJUST_INVENTORY"; }
    @Override public String entityType() { return "Product"; }
    @Override public AdjustmentResult execute() { return receiver.adjustStock(productId, request, userId); }
    @Override public String entityId(AdjustmentResult result) { return String.valueOf(productId); }

    @Override
    public String details() {
        return request.type() + " " + request.quantity() + " (" + request.reason() + ")";
    }
}
