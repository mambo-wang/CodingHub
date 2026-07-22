package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.mcp.McpNotificationService;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ToolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;
    private final McpNotificationService mcpNotificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ToolSummaryDTO>>> getTools(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "hot") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        PageResponse<ToolSummaryDTO> response = toolService.getTools(categoryId, keyword, tagId, sortBy, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolDetailDTO>> getToolById(@PathVariable Long id) {
        ToolDetailDTO tool = toolService.getToolById(id);
        return ResponseEntity.ok(ApiResponse.success(tool));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ToolSummaryDTO>> createTool(
            @Valid @RequestBody CreateToolRequest request,
            @AuthenticationPrincipal User currentUser) {

        ToolSummaryDTO tool = toolService.createTool(request, currentUser.getId());
        mcpNotificationService.notifyToolCreated(tool.getId(), tool.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("上传成功", tool));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolDetailDTO>> updateTool(
            @PathVariable Long id,
            @Valid @RequestBody UpdateToolRequest request,
            @AuthenticationPrincipal User currentUser) {

        ToolDetailDTO tool = toolService.updateTool(id, request, currentUser);
        mcpNotificationService.notifyToolUpdated(id, tool.getName());
        return ResponseEntity.ok(ApiResponse.success("更新成功", tool));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTool(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        toolService.deleteTool(id, currentUser);
        mcpNotificationService.notifyToolDeleted(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/{id}/logo")
    public ResponseEntity<ApiResponse<Void>> updateLogo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLogoRequest request,
            @AuthenticationPrincipal User currentUser) {

        toolService.updateLogo(id, request.getLogoUrl(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Logo更新成功", null));
    }

    @PostMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> pinTool(@PathVariable Long id) {
        toolService.pinTool(id);
        return ResponseEntity.ok(ApiResponse.success("置顶成功", null));
    }

    @DeleteMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unpinTool(@PathVariable Long id) {
        toolService.unpinTool(id);
        return ResponseEntity.ok(ApiResponse.success("取消置顶成功", null));
    }

    @GetMapping("/hot-top5")
    public ResponseEntity<ApiResponse<List<Long>>> getHotTop5() {
        List<Long> top5 = toolService.getHotTop5();
        return ResponseEntity.ok(ApiResponse.success(top5));
    }
}
