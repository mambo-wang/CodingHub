package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.CategoryDTO;
import com.iaihub.toolbox.dto.UpdateLogoRequest;
import com.iaihub.toolbox.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PutMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategoryLogo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLogoRequest request) {
        CategoryDTO category = categoryService.updateLogo(id, request.getLogoUrl());
        return ResponseEntity.ok(ApiResponse.success("分类 Logo 更新成功", category));
    }
}
