package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByStatusOrderByCreatedAtDesc(VideoStatus status, Pageable pageable);

    Page<Video> findByUploaderIdAndStatusOrderByCreatedAtDesc(Long uploaderId, VideoStatus status, Pageable pageable);

    Optional<Video> findByIdAndStatus(Long id, VideoStatus status);
}
