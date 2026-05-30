package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumPostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostTagRepository extends JpaRepository<ForumPostTag, Long> {
}