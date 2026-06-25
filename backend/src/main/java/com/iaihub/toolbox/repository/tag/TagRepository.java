package com.iaihub.toolbox.repository.tag;

import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByTagType(TagType tagType);

    Optional<Tag> findByNameAndTagType(String name, TagType tagType);

    @Query("SELECT t FROM Tag t WHERE t.tagType = :tagType ORDER BY t.usageCount DESC")
    List<Tag> findTopByTagTypeOrderByUsageCountDesc(@Param("tagType") TagType tagType,
                                                     org.springframework.data.domain.Pageable pageable);

    boolean existsByNameAndTagType(String name, TagType tagType);
}
