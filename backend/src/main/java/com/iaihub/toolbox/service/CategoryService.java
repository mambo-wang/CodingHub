package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.CategoryDTO;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CategoryDTO toDTO(Category category) {
        String name = category.getName();
        // 将 "API" 统一改为 "插件"
        if ("API".equals(name)) {
            name = "插件";
        }
        return CategoryDTO.builder()
                .id(category.getId())
                .name(name)
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .build();
    }
}
