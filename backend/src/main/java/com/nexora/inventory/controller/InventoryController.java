package com.nexora.inventory.controller;

import com.nexora.inventory.command.AdjustInventoryCommand;
import com.nexora.inventory.command.CreateProductCommand;
import com.nexora.inventory.command.UpdateProductCommand;
import com.nexora.inventory.dto.InventoryDtos.AdjustStockRequest;
import com.nexora.inventory.dto.InventoryDtos.AdjustmentResult;
import com.nexora.inventory.dto.InventoryDtos.CategoryDto;
import com.nexora.inventory.dto.InventoryDtos.CategoryRequest;
import com.nexora.inventory.dto.InventoryDtos.InventoryStats;
import com.nexora.inventory.dto.InventoryDtos.ProductDto;
import com.nexora.inventory.dto.InventoryDtos.ProductRequest;
import com.nexora.inventory.dto.InventoryDtos.StockAlertDto;
import com.nexora.inventory.dto.InventoryDtos.StrategyInfo;
import com.nexora.inventory.dto.InventoryDtos.SupplierDto;
import com.nexora.inventory.dto.InventoryDtos.SupplierRequest;
import com.nexora.inventory.dto.InventoryDtos.TransactionDto;
import com.nexora.inventory.entity.MovementType;
import com.nexora.inventory.service.CatalogService;
import com.nexora.inventory.service.InventoryService;
import com.nexora.inventory.strategy.InventoryStrategyResolver;
import com.nexora.shared.command.Command;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.security.CurrentUser;
import com.nexora.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
public class InventoryController {

    private final InventoryService inventory;
    private final CatalogService catalog;
    private final InventoryStrategyResolver strategies;
    private final CommandInvoker invoker;

    public InventoryController(InventoryService inventory, CatalogService catalog, InventoryStrategyResolver strategies,
                               CommandInvoker invoker) {
        this.inventory = inventory;
        this.catalog = catalog;
        this.strategies = strategies;
        this.invoker = invoker;
    }

    @GetMapping("/products")
    public PageResponse<ProductDto> products(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) String availability,
                                             @RequestParam(required = false) BigDecimal minValue,
                                             @RequestParam(required = false) BigDecimal maxValue,
                                             @RequestParam(defaultValue = "false") boolean includeInactive,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String sort) {
        return inventory.search(new InventoryService.ProductFilter(q, categoryId, availability, minValue, maxValue,
                includeInactive), page, size, sort);
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return inventory.get(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return invoker.run(new CreateProductCommand(inventory, request, CurrentUser.require().id()));
    }

    @PutMapping("/products/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return invoker.run(new UpdateProductCommand(inventory, id, request, CurrentUser.require().id()));
    }

    @PatchMapping("/products/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductDto setActive(@PathVariable Long id, @RequestParam boolean value) {
        return invoker.run(new Command<>() {
            @Override public String name() { return value ? "RESTORE_PRODUCT" : "ARCHIVE_PRODUCT"; }
            @Override public String entityType() { return "Product"; }
            @Override public ProductDto execute() { return inventory.setActive(id, value); }
            @Override public String entityId(ProductDto r) { return String.valueOf(id); }
        });
    }

    @PostMapping("/products/{id}/adjustments")
    public AdjustmentResult adjust(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request) {
        return invoker.run(new AdjustInventoryCommand(inventory, id, request, CurrentUser.require().id()));
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionDto> transactions(@RequestParam(required = false) Long productId,
                                                     @RequestParam(required = false) MovementType type,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "15") int size) {
        return inventory.transactions(productId, type, page, size);
    }

    @GetMapping("/alerts")
    public List<StockAlertDto> alerts(@RequestParam(defaultValue = "true") boolean openOnly) {
        return inventory.alerts(openOnly);
    }

    @GetMapping("/stats")
    public InventoryStats stats() {
        return inventory.stats();
    }

    @GetMapping("/strategies")
    public List<StrategyInfo> strategies() {
        String active = strategies.activeDetection().key();
        return Stream.concat(
                strategies.allDetection().stream().map(s -> new StrategyInfo(s.key(), s.description(), s.key().equals(active))),
                strategies.allReplenishment().stream().map(s -> new StrategyInfo(s.policy().name(), s.description(), true)))
                .toList();
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalog.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(request);
    }

    @GetMapping("/suppliers")
    public List<SupplierDto> suppliers() {
        return catalog.suppliers();
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SupplierDto createSupplier(@Valid @RequestBody SupplierRequest request) {
        return catalog.createSupplier(request);
    }
}
