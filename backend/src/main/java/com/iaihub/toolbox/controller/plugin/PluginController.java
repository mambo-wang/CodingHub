package com.iaihub.toolbox.controller.plugin;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.plugin.PluginCreateDraftRequest;
import com.iaihub.toolbox.dto.plugin.PluginDetailDTO;
import com.iaihub.toolbox.dto.plugin.PluginSummaryDTO;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.plugin.PluginService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PluginSummaryDTO>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "new") String sort) {
        return ResponseEntity.ok(ApiResponse.success(pluginService.list(keyword, tagId, page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PluginDetailDTO>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(pluginService.getDetail(id)));
    }

    @GetMapping("/hot-top5")
    public ResponseEntity<ApiResponse<List<Long>>> getHotTop5() {
        return ResponseEntity.ok(ApiResponse.success(pluginService.getHotTop5()));
    }

    @PostMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> pinPlugin(@PathVariable Long id) {
        pluginService.pinPlugin(id);
        return ResponseEntity.ok(ApiResponse.success("置顶成功", null));
    }

    @DeleteMapping("/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unpinPlugin(@PathVariable Long id) {
        pluginService.unpinPlugin(id);
        return ResponseEntity.ok(ApiResponse.success("取消置顶成功", null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PluginDetailDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "logoUrl", required = false) String logoUrl,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
            @AuthenticationPrincipal User currentUser) {
        PluginDetailDTO plugin = pluginService.upload(file, source, logoUrl, currentUser.getId(), tagIds);
        return ResponseEntity.status(201).body(ApiResponse.created("上传成功", plugin));
    }

    /**
     * MCP 两段式创建第一步：保存插件元数据（草稿，不含 zip）。
     */
    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<PluginDetailDTO>> createDraft(
            @RequestBody @Valid PluginCreateDraftRequest request,
            @AuthenticationPrincipal User currentUser) {
        PluginDetailDTO plugin = pluginService.createDraft(request, currentUser.getId());
        return ResponseEntity.status(201).body(ApiResponse.created("插件草稿已创建", plugin));
    }

    /**
     * MCP 两段式创建第二步：为草稿插件补全 zip，校验后转为正式发布。
     */
    @PostMapping("/{id}/file")
    public ResponseEntity<ApiResponse<PluginDetailDTO>> finalizeUpload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        // 免认证补全（对齐 tool 文件上传策略）。安全性由 zip 内 name/version 与草稿一致的校验保证。
        PluginDetailDTO plugin = pluginService.finalizeUpload(id, file);
        return ResponseEntity.ok(ApiResponse.success("插件已发布", plugin));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PluginDetailDTO>> update(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "logoUrl", required = false) String logoUrl,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
            @AuthenticationPrincipal User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;
        PluginDetailDTO plugin = pluginService.update(id, file, source, logoUrl, currentUser.getId(), isAdmin, tagIds);
        return ResponseEntity.ok(ApiResponse.success("更新成功", plugin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;
        pluginService.delete(id, currentUser.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/{id}/download")
    public void downloadZip(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String filename = "plugin-" + id + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        pluginService.downloadZip(id, response.getOutputStream());
    }
}
