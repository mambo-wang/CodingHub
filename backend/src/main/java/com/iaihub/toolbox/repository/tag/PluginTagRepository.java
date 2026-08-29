package com.iaihub.toolbox.repository.tag;

import com.iaihub.toolbox.model.tag.PluginTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PluginTagRepository extends JpaRepository<PluginTag, PluginTag.PluginTagId> {
    List<PluginTag> findByPluginId(Long pluginId);
    void deleteByPluginId(Long pluginId);

    @Query("SELECT pt.pluginId FROM PluginTag pt WHERE pt.tagId = :tagId")
    List<Long> findPluginIdsByTagId(@Param("tagId") Long tagId);
}
