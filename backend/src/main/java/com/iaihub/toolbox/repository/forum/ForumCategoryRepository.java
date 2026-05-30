package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {

    List<ForumCategory> findAllByOrderBySortOrderAsc();
}