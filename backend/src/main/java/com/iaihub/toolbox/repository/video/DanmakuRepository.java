package com.iaihub.toolbox.repository.video;

import com.iaihub.toolbox.model.video.Danmaku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DanmakuRepository extends JpaRepository<Danmaku, Long> {
    List<Danmaku> findByVideoIdOrderByTimeSecondsAsc(Long videoId);
}
