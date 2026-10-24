package com.studenthub.repository;

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

import com.studenthub.entity.Note;

public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {

    /** Tags are loaded in batches (@BatchSize on Note.tags), not joined, so paging stays in SQL. */
    @Override
    @EntityGraph(attributePaths = "subject")
    Page<Note> findAll(Specification<Note> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"subject", "tags"})
    Optional<Note> findByIdAndUserId(Long id, Long userId);

    @Query("select distinct t from Note n join n.tags t where n.user.id = :userId order by t")
    List<String> findDistinctTagsByUserId(@Param("userId") Long userId);
}
