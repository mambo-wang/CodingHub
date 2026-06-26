package com.iaihub.toolbox.repository.feedback;

import com.iaihub.toolbox.model.feedback.FeedbackCategory;
import com.iaihub.toolbox.model.feedback.FeedbackMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    Page<FeedbackMessage> findByStatusOrderByCreatedAtDesc(FeedbackMessage.Status status, Pageable pageable);

    Page<FeedbackMessage> findByCategoryAndStatusOrderByCreatedAtDesc(
        FeedbackCategory category, FeedbackMessage.Status status, Pageable pageable);

    boolean existsByIdAndStatus(Long id, FeedbackMessage.Status status);

    @Query("SELECT f FROM FeedbackMessage f WHERE f.id = :id AND f.status = 'NORMAL'")
    Optional<FeedbackMessage> findByIdAndStatusNormal(@Param("id") Long id);
}
