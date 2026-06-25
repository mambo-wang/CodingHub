package com.iaihub.toolbox.repository.tag;

import com.iaihub.toolbox.model.tag.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoTagRepository extends JpaRepository<VideoTag, VideoTag.VideoTagId> {
    List<VideoTag> findByVideoId(Long videoId);
    void deleteByVideoId(Long videoId);
}
