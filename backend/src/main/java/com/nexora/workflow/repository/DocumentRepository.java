package com.nexora.workflow.repository;

import com.nexora.shared.security.Role;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.entity.DocumentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.id = :id")
    Optional<Document> lockById(@Param("id") Long id);

    @Query("""
            select d from Document d
            where (:authorId is null or d.authorId = :authorId)
              and (:type is null or d.type = :type)
              and (:status is null or d.status = :status)
              and (:from is null or d.createdAt >= :from)
              and (:to is null or d.createdAt < :to)
              and (:q is null or lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(d.number) like lower(concat('%', :q, '%')))
            """)
    Page<Document> search(@Param("authorId") Long authorId, @Param("type") DocumentType type,
                          @Param("status") DocumentStatus status, @Param("from") Instant from,
                          @Param("to") Instant to, @Param("q") String q, Pageable pageable);

    /** Documents whose active step can be decided by the given roles (the "awaiting my action" inbox). */
    @Query("""
            select d from Document d
            where d.status in :statuses and d.authorId <> :userId and exists (
                select s.id from ApprovalStep s where s.documentId = d.id and s.approvalRound = d.approvalRound
                  and s.status = com.nexora.workflow.entity.ApprovalStep.Status.ACTIVE and s.requiredRole in :roles)
            order by d.submittedAt asc
            """)
    List<Document> awaitingAction(@Param("roles") Collection<Role> roles, @Param("userId") Long userId,
                                  @Param("statuses") Collection<DocumentStatus> statuses);

    long countByStatusIn(Collection<DocumentStatus> statuses);

    @Query("select d.status, count(d) from Document d group by d.status")
    List<Object[]> countGroupedByStatus();

    List<Document> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, Long relatedEntityId);
}
