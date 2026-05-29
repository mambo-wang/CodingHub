package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ToolFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolFileRepository extends JpaRepository<ToolFile, Long> {

    List<ToolFile> findByToolId(Long toolId);

    @Query("SELECT tf FROM ToolFile tf WHERE tf.toolId = :toolId AND tf.status = 'NORMAL'")
    List<ToolFile> findByToolIdAndStatusNormal(@Param("toolId") Long toolId);

    Optional<ToolFile> findByIdAndToolId(Long id, Long toolId);

    @Modifying
    @Query("DELETE FROM ToolFile tf WHERE tf.toolId = :toolId")
    void deleteByToolId(@Param("toolId") Long toolId);
}
