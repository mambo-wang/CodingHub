package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.VideoFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Deprecated
public interface VideoFavoriteRepository extends JpaRepository<VideoFavorite, Long> {

    Page<VideoFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<VideoFavorite> findByVideoIdAndUserId(Long videoId, Long userId);

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    void deleteByVideoIdAndUserId(Long videoId, Long userId);
}
