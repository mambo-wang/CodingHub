package com.iaihub.toolbox.repository.kb;

import com.iaihub.toolbox.model.kb.KbDocument;
import com.iaihub.toolbox.model.kb.KbStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {

    List<KbDocument> findByKbIdAndStatusOrderByCreatedAtDesc(Long kbId, KbStatus status);

    Optional<KbDocument> findByIdAndKbIdAndStatus(Long id, Long kbId, KbStatus status);

    long countByKbIdAndStatus(Long kbId, KbStatus status);
}
