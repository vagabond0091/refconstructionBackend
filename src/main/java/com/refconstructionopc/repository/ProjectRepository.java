package com.refconstructionopc.repository;

import com.refconstructionopc.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByUniqueId(String uniqueId);
    boolean existsByUniqueId(String uniqueId);
    void deleteByUniqueId(String uniqueId);

    @Query("""
    SELECT p FROM Project p
    WHERE (:search IS NULL OR :search = '' OR
           LOWER(p.title)       LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(p.uniqueId)    LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(p.serviceType) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY p.createdAt DESC
  """)
    List<Project> getAllProjectWithSearch(@Param("search") String search);
}