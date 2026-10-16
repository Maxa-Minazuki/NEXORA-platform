package com.nexora.inventory.repository;

import com.nexora.inventory.entity.InventoryTransaction;
import com.nexora.inventory.entity.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @EntityGraph(attributePaths = "product")
    @Query("""
            select t from InventoryTransaction t
            where (:productId is null or t.product.id = :productId)
              and (:type is null or t.movementType = :type)
            """)
    Page<InventoryTransaction> search(@Param("productId") Long productId, @Param("type") MovementType type,
                                      Pageable pageable);

    /** Total units that left the warehouse for a product since {@code since} (positive number). */
    @Query("""
            select coalesce(-sum(t.quantityChange), 0) from InventoryTransaction t
            where t.product.id = :productId and t.quantityChange < 0 and t.createdAt >= :since
            """)
    long outflowSince(@Param("productId") Long productId, @Param("since") Instant since);

    @Query("select t from InventoryTransaction t where t.createdAt >= :since")
    List<InventoryTransaction> findSince(@Param("since") Instant since);
}
