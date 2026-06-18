package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {

    Optional<VideoLike> findByVideoIdAndUserId(Long videoId, Long userId);

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    void deleteByVideoIdAndUserId(Long videoId, Long userId);
}
