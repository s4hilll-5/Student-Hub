package com.studenthub.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studenthub.entity.Assignment;
import com.studenthub.entity.AssignmentStatus;

public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    /** Same as the inherited method, but fetches the subject in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = "subject")
    Page<Assignment> findAll(Specification<Assignment> spec, Pageable pageable);

    @EntityGraph(attributePaths = "subject")
    Optional<Assignment> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, AssignmentStatus status);

    /** Overdue: due before now and not completed. */
    long countByUserIdAndStatusNotAndDueAtBefore(Long userId, AssignmentStatus status, Instant now);

    @EntityGraph(attributePaths = "subject")
    List<Assignment> findTop5ByUserIdAndStatusNotAndDueAtGreaterThanEqualOrderByDueAtAsc(
            Long userId, AssignmentStatus status, Instant now);

    @EntityGraph(attributePaths = "subject")
    List<Assignment> findByUserIdAndStatusNotAndDueAtBetweenOrderByDueAtAsc(
            Long userId, AssignmentStatus status, Instant from, Instant to);

    @Query("select a.completedAt from Assignment a where a.user.id = :userId and a.completedAt >= :from")
    List<Instant> findCompletedAtSince(@Param("userId") Long userId, @Param("from") Instant from);

    @Query("select a.createdAt from Assignment a where a.user.id = :userId and a.createdAt >= :from")
    List<Instant> findCreatedAtSince(@Param("userId") Long userId, @Param("from") Instant from);
}
