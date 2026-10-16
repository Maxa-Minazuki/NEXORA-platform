package com.nexora.inventory.service;

import com.nexora.inventory.dto.InventoryDtos.AdjustStockRequest;
import com.nexora.inventory.dto.InventoryDtos.AdjustmentResult;
import com.nexora.inventory.dto.InventoryDtos.CategoryValue;
import com.nexora.inventory.dto.InventoryDtos.InventoryStats;
import com.nexora.inventory.dto.InventoryDtos.ProductDto;
import com.nexora.inventory.dto.InventoryDtos.ProductRequest;
import com.nexora.inventory.dto.InventoryDtos.StockAlertDto;
import com.nexora.inventory.dto.InventoryDtos.TransactionDto;
import com.nexora.inventory.entity.Category;
import com.nexora.inventory.entity.InventoryTransaction;
import com.nexora.inventory.entity.MovementType;
import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.StockAlert;
import com.nexora.inventory.entity.Supplier;
import com.nexora.inventory.repository.CategoryRepository;
import com.nexora.inventory.repository.InventoryTransactionRepository;
import com.nexora.inventory.repository.ProductRepository;
import com.nexora.inventory.repository.StockAlertRepository;
import com.nexora.inventory.repository.SupplierRepository;
import com.nexora.inventory.strategy.InventoryStrategyResolver;
import com.nexora.inventory.strategy.LowStockDetectionStrategy;
import com.nexora.shared.event.LowStockEvent;
import com.nexora.shared.event.StockAdjustedEvent;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.ConflictException;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Receiver for inventory commands. All stock changes go through {@link #adjustStock}, which locks the
 * product row, writes a ledger line and publishes Observer events (stock adjusted / low stock).
 */
@Service
public class InventoryService {

    private static final Set<String> SORTABLE = Set.of("name", "sku", "quantity", "minStock", "retailPrice",
            "purchasePrice", "updatedAt");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final SupplierRepository suppliers;
    private final InventoryTransactionRepository transactions;
    private final StockAlertRepository alerts;
    private final InventoryStrategyResolver strategies;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public InventoryService(ProductRepository products, CategoryRepository categories, SupplierRepository suppliers,
                            InventoryTransactionRepository transactions, StockAlertRepository alerts,
                            InventoryStrategyResolver strategies, ApplicationEventPublisher events, Clock clock) {
        this.products = products;
        this.categories = categories;
        this.suppliers = suppliers;
        this.transactions = transactions;
        this.alerts = alerts;
        this.strategies = strategies;
        this.events = events;
        this.clock = clock;
    }

    public record ProductFilter(String q, Long categoryId, String availability, BigDecimal minValue,
                                BigDecimal maxValue, boolean includeInactive) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(ProductFilter filter, int page, int size, String sort) {
        LowStockDetectionStrategy detection = strategies.activeDetection();
        Specification<Product> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("category");
                root.fetch("supplier", jakarta.persistence.criteria.JoinType.LEFT);
            }
            return cb.conjunction();
        };
        if (!filter.includeInactive()) {
            spec = spec.and((r, q, cb) -> cb.isTrue(r.get("active")));
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            String like = "%" + filter.q().trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("name")), like),
                    cb.like(cb.lower(r.get("sku")), like)));
        }
        if (filter.categoryId() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("category").get("id"), filter.categoryId()));
        }
        if (filter.availability() != null) {
            spec = switch (filter.availability()) {
                case "LOW_STOCK" -> spec.and(detection.criticalSpecification())
                        .and((r, q, cb) -> cb.greaterThan(r.get("quantity"), 0));
                case "OUT_OF_STOCK" -> spec.and((r, q, cb) -> cb.equal(r.get("quantity"), 0));
                case "IN_STOCK" -> spec.and(Specification.not(detection.criticalSpecification()));
                case "CRITICAL" -> spec.and(detection.criticalSpecification());
                default -> spec;
            };
        }
        if (filter.minValue() != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(stockValue(r, cb), filter.minValue()));
        }
        if (filter.maxValue() != null) {
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(stockValue(r, cb), filter.maxValue()));
        }
        var pageable = PageRequests.of(page, size, sort, SORTABLE, "name");
        return PageResponse.of(products.findAll(spec, pageable), p -> InventoryMapper.toDto(p, detection));
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return InventoryMapper.toDto(products.findWithRelationsById(id)
                .orElseThrow(() -> new NotFoundException("Product", id)), strategies.activeDetection());
    }

    @Transactional
    public ProductDto create(ProductRequest req, Long userId) {
        if (products.existsBySkuIgnoreCase(req.sku())) {
            throw new ConflictException("DUPLICATE_SKU", "SKU " + req.sku() + " бар тауар бұрыннан бар");
        }
        Product product = new Product(req.sku().toUpperCase(), req.name(), category(req.categoryId()));
        applyRequest(product, req);
        products.save(product);
        if (req.quantity() > 0) {
            product.applyMovement(req.quantity());
            transactions.save(new InventoryTransaction(product, MovementType.INITIAL, req.quantity(), 0,
                    req.quantity(), "Бастапқы қалдық", userId, clock.instant()));
        }
        evaluateStockLevel(product, Integer.MAX_VALUE, userId);
        return InventoryMapper.toDto(product, strategies.activeDetection());
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest req, Long userId) {
        Product product = products.lockById(id).orElseThrow(() -> new NotFoundException("Product", id));
        if (!product.getSku().equalsIgnoreCase(req.sku())) {
            throw new BusinessRuleException("SKU_IMMUTABLE", "Тауар жасалғаннан кейін SKU өзгертілмейді");
        }
        int before = product.getQuantity();
        int previousMin = product.getMinStock();
        applyRequest(product, req);
        // The quantity field of the form is ignored on update: stock only changes through movements.
        LowStockDetectionStrategy detection = strategies.activeDetection();
        boolean wasCritical = detection.isCritical(before, previousMin);
        evaluateStockLevel(product, wasCritical ? 0 : Integer.MAX_VALUE, userId);
        return InventoryMapper.toDto(product, detection);
    }

    @Transactional
    public ProductDto setActive(Long id, boolean active) {
        Product product = products.findWithRelationsById(id).orElseThrow(() -> new NotFoundException("Product", id));
        product.setActive(active);
        return InventoryMapper.toDto(product, strategies.activeDetection());
    }

    /**
     * The single entry point for stock movements. {@code RECEIPT} adds, {@code WRITE_OFF} removes and
     * {@code ADJUSTMENT} applies a signed correction.
     */
    @Transactional
    public AdjustmentResult adjustStock(Long productId, AdjustStockRequest req, Long userId) {
        int delta = switch (req.type()) {
            case RECEIPT -> positive(req.quantity());
            case WRITE_OFF -> -positive(req.quantity());
            case ADJUSTMENT -> {
                if (req.quantity() == 0) {
                    throw new BusinessRuleException("ZERO_ADJUSTMENT", "Түзету мөлшері нөлге тең болмауы керек");
                }
                yield req.quantity();
            }
            case INITIAL -> throw new BusinessRuleException("INVALID_MOVEMENT", "Бастапқы қалдық қозғалысы автоматты түрде жасалады");
        };
        Product product = products.lockById(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        if (!product.isActive()) {
            throw new BusinessRuleException("PRODUCT_INACTIVE", "Тауар " + product.getSku() + " мұрағатқа жіберілген");
        }
        int before = product.getQuantity();
        product.applyMovement(delta);
        InventoryTransaction tx = transactions.save(new InventoryTransaction(product, req.type(), delta, before,
                product.getQuantity(), req.reason(), userId, clock.instant()));
        events.publishEvent(new StockAdjustedEvent(product.getId(), product.getSku(), product.getName(),
                req.type().name(), delta, product.getQuantity(), userId, clock.instant()));
        boolean triggered = evaluateStockLevel(product, before, userId);
        LowStockDetectionStrategy detection = strategies.activeDetection();
        return new AdjustmentResult(InventoryMapper.toDto(product, detection), InventoryMapper.toDto(tx), triggered,
                detection.key());
    }

    /**
     * Publishes {@link LowStockEvent} when the product crosses into the critical zone and no alert is
     * open yet; resolves open alerts once stock is healthy again.
     */
    private boolean evaluateStockLevel(Product product, int quantityBefore, Long userId) {
        LowStockDetectionStrategy detection = strategies.activeDetection();
        boolean nowCritical = detection.isCritical(product.getQuantity(), product.getMinStock());
        if (!nowCritical) {
            alerts.findByProductIdAndStatus(product.getId(), StockAlert.Status.OPEN)
                    .forEach(a -> a.resolve(clock.instant()));
            alerts.findByProductIdAndStatus(product.getId(), StockAlert.Status.REQUEST_CREATED)
                    .forEach(a -> a.resolve(clock.instant()));
            return false;
        }
        // One alert per critical episode: an open alert (or one with a pending request) suppresses duplicates.
        boolean alreadyOpen = alerts.existsByProductIdAndStatusIn(product.getId(),
                EnumSet.of(StockAlert.Status.OPEN, StockAlert.Status.REQUEST_CREATED));
        if (alreadyOpen) {
            return false;
        }
        var replenishment = strategies.replenishment(product.getReplenishmentPolicy());
        events.publishEvent(new LowStockEvent(product.getId(), product.getSku(), product.getName(),
                product.getQuantity(), detection.criticalThreshold(product.getMinStock()),
                replenishment.suggestQuantity(product), product.getReplenishmentPolicy().name(), userId,
                clock.instant()));
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> transactions(Long productId, MovementType type, int page, int size) {
        var pageable = PageRequests.of(page, size, null, Set.of(), "-createdAt");
        return PageResponse.of(transactions.search(productId, type, pageable), InventoryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<StockAlertDto> alerts(boolean openOnly) {
        var list = openOnly
                ? alerts.findByStatusInOrderByCreatedAtDesc(EnumSet.of(StockAlert.Status.OPEN, StockAlert.Status.REQUEST_CREATED))
                : alerts.findTop50ByOrderByCreatedAtDesc();
        return list.stream().map(InventoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StockAlert requireAlert(Long alertId) {
        StockAlert alert = alerts.findById(alertId).orElseThrow(() -> new NotFoundException("Stock alert", alertId));
        alert.getProduct().getName(); // initialise for callers outside the transaction
        return alert;
    }

    /** Called by the workflow module once a replenishment request document exists for the alert. */
    @Transactional
    public void linkReplenishmentRequest(Long alertId, Long documentId) {
        StockAlert alert = alerts.findById(alertId).orElseThrow(() -> new NotFoundException("Stock alert", alertId));
        if (alert.getStatus() != StockAlert.Status.OPEN) {
            throw new ConflictException("ALERT_NOT_OPEN", "Бұл ескерту бойынша толықтыру өтінімі бұрыннан бар");
        }
        alert.linkReplenishmentRequest(documentId);
    }

    @Transactional(readOnly = true)
    public InventoryStats stats() {
        LowStockDetectionStrategy detection = strategies.activeDetection();
        long critical = products.count(detection.criticalSpecification().and((r, q, cb) -> cb.isTrue(r.get("active"))));
        long out = products.countOutOfStock();
        List<CategoryValue> byCategory = products.valueByCategory().stream()
                .map(row -> new CategoryValue((String) row[0], ((Number) row[1]).longValue(), toBigDecimal(row[2])))
                .toList();
        return new InventoryStats(products.countByActiveTrue(), products.totalUnits(),
                scale(products.totalStockValue()), scale(products.totalRetailValue()), critical - out, out,
                alerts.countByStatus(StockAlert.Status.OPEN), detection.key(), byCategory);
    }

    @SuppressWarnings("unchecked")
    private static jakarta.persistence.criteria.Expression<BigDecimal> stockValue(
            jakarta.persistence.criteria.Root<Product> r, jakarta.persistence.criteria.CriteriaBuilder cb) {
        return (jakarta.persistence.criteria.Expression<BigDecimal>) (jakarta.persistence.criteria.Expression<?>)
                cb.prod(r.<Integer>get("quantity"), r.<BigDecimal>get("purchasePrice"));
    }

    private void applyRequest(Product product, ProductRequest req) {
        if (req.retailPrice().compareTo(req.purchasePrice()) < 0) {
            throw new BusinessRuleException("PRICE_BELOW_COST", "Бөлшек баға сатып алу бағасынан төмен болмауы керек");
        }
        Supplier supplier = req.supplierId() == null ? null : suppliers.findById(req.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier", req.supplierId()));
        product.update(req.name().trim(), req.description(), category(req.categoryId()), supplier,
                req.purchasePrice(), req.retailPrice(), req.minStock(), req.unit().trim(), req.replenishmentStrategy());
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    }

    private static int positive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Мөлшер нөлден үлкен болуы керек");
        }
        return quantity;
    }

    private static BigDecimal toBigDecimal(Object o) {
        return o instanceof BigDecimal b ? scale(b) : scale(new BigDecimal(o.toString()));
    }

    private static BigDecimal scale(BigDecimal b) {
        return b.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
