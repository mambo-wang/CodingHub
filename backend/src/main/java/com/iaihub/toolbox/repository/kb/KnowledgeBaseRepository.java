package com.iaihub.toolbox.repository.kb;

import com.iaihub.toolbox.model.kb.KbStatus;
import com.iaihub.toolbox.model.kb.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Page<KnowledgeBase> findByStatusOrderByCreatedAtDesc(KbStatus status, Pageable pageable);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.status = :status ORDER BY kb.createdAt DESC")
    Page<KnowledgeBase> findByStatusOrderByHot(@Param("status") KbStatus status, Pageable pageable);

    Page<KnowledgeBase> findByOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, KbStatus status, Pageable pageable);

    Optional<KnowledgeBase> findByIdAndStatus(Long id, KbStatus status);

    Optional<KnowledgeBase> findByNameAndStatus(String name, KbStatus status);

    boolean existsByNameAndStatus(String name, KbStatus status);

}
