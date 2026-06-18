package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.VideoComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    Page<VideoComment> findByVideoIdOrderByCreatedAtDesc(Long videoId, Pageable pageable);

    long countByVideoId(Long videoId);
}
