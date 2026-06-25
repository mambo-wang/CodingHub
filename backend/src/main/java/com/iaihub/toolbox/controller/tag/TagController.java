package com.iaihub.toolbox.controller.tag;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.tag.CreateTagRequest;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.service.tag.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagDTO>>> getTagsByType(
            @RequestParam(required = false, defaultValue = "TOOL") String type) {
        List<TagDTO> tags = tagService.getTagsByType(type);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<TagDTO>>> getHotTags(
            @RequestParam(required = false, defaultValue = "TOOL") String type,
            @RequestParam(defaultValue = "20") int limit) {
        List<TagDTO> tags = tagService.getHotTags(type, limit);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TagDTO>> createTag(@Valid @RequestBody CreateTagRequest request) {
        TagDTO tag = tagService.createTag(request.getName(), request.getType());
        return ResponseEntity.ok(ApiResponse.success("标签创建成功", tag));
    }
}
