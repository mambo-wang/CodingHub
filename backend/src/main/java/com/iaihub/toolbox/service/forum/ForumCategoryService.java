package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumCategoryDTO;
import com.iaihub.toolbox.repository.forum.ForumCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumCategoryService {

    private final ForumCategoryRepository categoryRepository;

    public List<ForumCategoryDTO> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc()
            .stream()
            .map(c -> new ForumCategoryDTO(c.getId(), c.getName(), c.getDescription(), c.getSortOrder(), 0))
            .toList();
    }
}