package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumCategoryDTO;
import com.iaihub.toolbox.service.forum.ForumCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/categories")
@RequiredArgsConstructor
public class ForumCategoryController {

    private final ForumCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<ForumCategoryDTO>> getAllCategories() {
        List<ForumCategoryDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}