package com.nexora.audit.repository;

import com.nexora.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:outcome is null or a.outcome = :outcome)
              and (:actor is null or lower(a.actorUsername) like lower(concat('%', :actor, '%')))
            """)
    Page<AuditLog> search(@Param("action") String action, @Param("entityType") String entityType,
                          @Param("outcome") String outcome, @Param("actor") String actor, Pageable pageable);

    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    @Query("select distinct a.action from AuditLog a order by a.action")
    List<String> distinctActions();
}
