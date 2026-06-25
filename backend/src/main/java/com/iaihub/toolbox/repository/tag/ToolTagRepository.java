package com.iaihub.toolbox.repository.tag;

import com.iaihub.toolbox.model.tag.ToolTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolTagRepository extends JpaRepository<ToolTag, ToolTag.ToolTagId> {
    List<ToolTag> findByToolId(Long toolId);
    void deleteByToolId(Long toolId);
}
