package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByStatusOrderByCreatedAtDesc(VideoStatus status, Pageable pageable);

    Page<Video> findByUploaderIdAndStatusOrderByCreatedAtDesc(Long uploaderId, VideoStatus status, Pageable pageable);

    Optional<Video> findByIdAndStatus(Long id, VideoStatus status);

    List<Video> findTop20ByStatusOrderByViewCountDesc(VideoStatus status);

    // 热度排序查询：pinned DESC, score DESC
    @Query("SELECT v FROM Video v WHERE v.status = :status ORDER BY v.pinned DESC, v.score DESC")
    Page<Video> findByStatusOrderByHot(@Param("status") VideoStatus status, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.uploaderId = :uploaderId AND v.status = :status ORDER BY v.pinned DESC, v.score DESC")
    Page<Video> findByUploaderIdAndStatusOrderByHot(@Param("uploaderId") Long uploaderId,
                                                     @Param("status") VideoStatus status,
                                                     Pageable pageable);

    // 热度 Top5
    @Query("SELECT v.id FROM Video v WHERE v.status = 'NORMAL' ORDER BY v.score DESC")
    List<Long> findTop5ByStatusOrderByScoreDesc(Pageable pageable);

    // 置顶/取消置顶
    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.pinned = true WHERE v.id = :id")
    int pinById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.pinned = false WHERE v.id = :id")
    int unpinById(@Param("id") Long id);
}
