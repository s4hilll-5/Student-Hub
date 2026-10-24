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

import com.studenthub.entity.Examination;

public interface ExaminationRepository extends JpaRepository<Examination, Long>, JpaSpecificationExecutor<Examination> {

    @Override
    @EntityGraph(attributePaths = "subject")
    Page<Examination> findAll(Specification<Examination> spec, Pageable pageable);

    @EntityGraph(attributePaths = "subject")
    Optional<Examination> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "subject")
    List<Examination> findTop5ByUserIdAndExamAtGreaterThanEqualOrderByExamAtAsc(Long userId, Instant now);

    @EntityGraph(attributePaths = "subject")
    List<Examination> findByUserIdAndExamAtBetweenOrderByExamAtAsc(Long userId, Instant from, Instant to);
}
