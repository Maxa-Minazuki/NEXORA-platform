package com.nexora.inventory.repository;

import com.nexora.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    boolean existsByProductIdAndStatusIn(Long productId, Collection<StockAlert.Status> statuses);

    List<StockAlert> findByProductIdAndStatus(Long productId, StockAlert.Status status);

    @EntityGraph(attributePaths = "product")
    List<StockAlert> findByStatusInOrderByCreatedAtDesc(Collection<StockAlert.Status> statuses);

    @EntityGraph(attributePaths = "product")
    List<StockAlert> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(StockAlert.Status status);
}
