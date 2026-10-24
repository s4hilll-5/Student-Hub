package com.studenthub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studenthub.entity.Subject;

/** Every lookup is scoped by user id, so one student can never load another student's subject. */
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByUserIdOrderByNameAsc(Long userId);

    Optional<Subject> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    boolean existsByUserIdAndCodeIgnoreCase(Long userId, String code);

    boolean existsByUserIdAndCodeIgnoreCaseAndIdNot(Long userId, String code, Long id);

    long countByUserId(Long userId);
}
