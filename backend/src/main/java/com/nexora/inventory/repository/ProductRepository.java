package com.nexora.inventory.repository;

import com.nexora.inventory.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** Row lock (SELECT ... FOR UPDATE) so concurrent stock movements on one product are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findWithRelationsById(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySku(String sku);

    long countByActiveTrue();

    @Query("select coalesce(sum(p.quantity * p.purchasePrice), 0) from Product p where p.active = true")
    BigDecimal totalStockValue();

    @Query("select coalesce(sum(p.quantity * p.retailPrice), 0) from Product p where p.active = true")
    BigDecimal totalRetailValue();

    @Query("select coalesce(sum(p.quantity), 0) from Product p where p.active = true")
    long totalUnits();

    @Query("select count(p) from Product p where p.active = true and p.quantity = 0")
    long countOutOfStock();

    @Query("""
            select p.category.name, count(p), coalesce(sum(p.quantity * p.purchasePrice), 0)
            from Product p where p.active = true group by p.category.name order by p.category.name
            """)
    List<Object[]> valueByCategory();
}
