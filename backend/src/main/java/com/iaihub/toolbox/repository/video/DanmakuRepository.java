package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.Danmaku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DanmakuRepository extends JpaRepository<Danmaku, Long> {
    @Query("SELECT d FROM Danmaku d JOIN FETCH d.user WHERE d.videoId = :videoId ORDER BY d.timeSeconds ASC")
    List<Danmaku> findByVideoIdWithUser(@Param("videoId") Long videoId);
}
