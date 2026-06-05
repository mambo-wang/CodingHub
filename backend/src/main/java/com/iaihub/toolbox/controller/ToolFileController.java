package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.FileListResponse;
import com.iaihub.toolbox.dto.FileUploadResponse;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ToolFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tools/{toolId}/files")
@RequiredArgsConstructor
@Slf4j
public class ToolFileController {

    private final ToolFileService toolFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadFiles(
            @PathVariable Long toolId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "readme", required = false) String readme,
            @AuthenticationPrincipal User currentUser) {

        log.info("Received file upload request for tool {}, files count: {}", toolId, files.size());

        FileUploadResponse response = toolFileService.uploadFiles(toolId, files, readme, currentUser.getId());

        return ApiResponse.success("文件上传成功", response);
    }

    @GetMapping
    public ApiResponse<FileListResponse> getToolFiles(@PathVariable Long toolId) {
        log.info("Getting files for tool {}", toolId);

        FileListResponse response = toolFileService.getToolFiles(toolId);

        return ApiResponse.success(response);
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteToolFile(
            @PathVariable Long toolId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {

        log.info("Deleting file {} for tool {}", fileId, toolId);

        toolFileService.deleteToolFile(toolId, fileId, currentUser.getId());

        return ApiResponse.success("文件删除成功", null);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long toolId,
            @PathVariable Long fileId) {

        log.info("Downloading file {} for tool {}", fileId, toolId);

        ToolFile toolFile = toolFileService.downloadFile(toolId, fileId);
        InputStream inputStream = toolFileService.getFileInputStream(toolId, fileId);

        String contentType = toolFile.getContentType() != null ? toolFile.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toolFile.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(toolFile.getFileSize())
                .body(new InputStreamResource(inputStream));
    }
}
