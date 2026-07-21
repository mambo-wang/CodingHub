package com.iaihub.toolbox.repository.tag;

import com.iaihub.toolbox.model.tag.ToolTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolTagRepository extends JpaRepository<ToolTag, ToolTag.ToolTagId> {
    List<ToolTag> findByToolId(Long toolId);
    void deleteByToolId(Long toolId);

    @Query("SELECT tt.toolId FROM ToolTag tt WHERE tt.tagId = :tagId")
    List<Long> findToolIdsByTagId(@Param("tagId") Long tagId);
}
