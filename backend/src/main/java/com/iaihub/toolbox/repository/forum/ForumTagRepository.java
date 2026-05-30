package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumTagRepository extends JpaRepository<ForumTag, Long> {

    Optional<ForumTag> findByName(String name);

    List<ForumTag> findTop10ByOrderByPostCountDesc();

    List<ForumTag> findByNameContaining(String keyword);
}