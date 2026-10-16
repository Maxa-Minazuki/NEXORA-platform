package com.nexora.inventory.service;

import com.nexora.inventory.dto.InventoryDtos.CategoryDto;
import com.nexora.inventory.dto.InventoryDtos.CategoryRequest;
import com.nexora.inventory.dto.InventoryDtos.SupplierDto;
import com.nexora.inventory.dto.InventoryDtos.SupplierRequest;
import com.nexora.inventory.entity.Category;
import com.nexora.inventory.entity.Supplier;
import com.nexora.inventory.repository.CategoryRepository;
import com.nexora.inventory.repository.SupplierRepository;
import com.nexora.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CategoryRepository categories;
    private final SupplierRepository suppliers;

    public CatalogService(CategoryRepository categories, SupplierRepository suppliers) {
        this.categories = categories;
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categories.findAllByOrderByNameAsc().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription())).toList();
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest req) {
        if (categories.existsByNameIgnoreCase(req.name().trim())) {
            throw new ConflictException("DUPLICATE_CATEGORY", "«" + req.name() + "» санаты бұрыннан бар");
        }
        Category c = categories.save(new Category(req.name().trim(), req.description()));
        return new CategoryDto(c.getId(), c.getName(), c.getDescription());
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> suppliers() {
        return suppliers.findAllByOrderByNameAsc().stream()
                .map(s -> new SupplierDto(s.getId(), s.getName(), s.getContactEmail(), s.getPhone())).toList();
    }

    @Transactional
    public SupplierDto createSupplier(SupplierRequest req) {
        if (suppliers.existsByNameIgnoreCase(req.name().trim())) {
            throw new ConflictException("DUPLICATE_SUPPLIER", "«" + req.name() + "» жеткізушісі бұрыннан бар");
        }
        Supplier s = suppliers.save(new Supplier(req.name().trim(), req.contactEmail(), req.phone()));
        return new SupplierDto(s.getId(), s.getName(), s.getContactEmail(), s.getPhone());
    }
}
