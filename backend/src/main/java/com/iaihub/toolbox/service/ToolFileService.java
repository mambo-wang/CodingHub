package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.FileListResponse;
import com.iaihub.toolbox.dto.FileUploadResponse;
import com.iaihub.toolbox.dto.ToolFileDTO;
import com.iaihub.toolbox.exception.FileValidationException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolFileService {

    private final ToolFileRepository toolFileRepository;
    private final ToolRepository toolRepository;
    private final UploadConfig uploadConfig;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50MB
    private static final long MAX_REQUEST_SIZE = 200 * 1024 * 1024L; // 200MB

    @Transactional
    public FileUploadResponse uploadFiles(Long toolId, List<MultipartFile> files, String readme, Long userId) {
        log.info("Uploading {} files for tool {}", files.size(), toolId);

        Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        // Verify ownership (skip if userId is null, e.g. anonymous upload via MCP client)
        if (userId != null && !tool.getUploader().getId().equals(userId)) {
            throw new ForbiddenException("您只能上传文件到自己的工具");
        }

        // Validate total size
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > MAX_REQUEST_SIZE) {
            throw new FileValidationException("总上传大小超过限制 (200MB)");
        }

        // Create tool folder
        Path toolFolder = Paths.get(uploadConfig.getBaseDir(), String.valueOf(toolId));
        try {
            Files.createDirectories(toolFolder);
        } catch (IOException e) {
            log.error("Failed to create tool folder: {}", toolFolder, e);
            throw new FileValidationException("无法创建工具文件夹");
        }

        List<ToolFileDTO> savedFiles = new ArrayList<>();
        boolean readmeSaved = false;

        // Save files
        for (MultipartFile file : files) {
            validateFile(file);

            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            Path targetPath = toolFolder.resolve(originalName);

            try {
                // Check for existing file with same name - if exists, delete old record
                toolFileRepository.findByToolIdAndOriginalNameAndStatus(toolId, originalName, ToolFile.Status.NORMAL)
                        .ifPresent(existing -> {
                            // Delete old physical file first
                            Path oldPath = Paths.get(uploadConfig.getBaseDir(), existing.getStoredPath());
                            try {
                                Files.deleteIfExists(oldPath);
                            } catch (IOException e) {
                                log.warn("Failed to delete old physical file: {}", oldPath, e);
                            }
                            // Physically delete old database record to free unique constraint
                            toolFileRepository.delete(existing);
                            // Flush to ensure unique constraint is released before inserting new record
                            toolFileRepository.flush();
                            log.info("Replaced existing file: {} for tool {}", originalName, toolId);
                        });

                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                String storedPath = Paths.get(String.valueOf(toolId), originalName).toString();

                ToolFile toolFile = ToolFile.builder()
                        .toolId(toolId)
                        .originalName(originalName)
                        .storedPath(storedPath)
                        .fileSize(file.getSize())
                        .contentType(file.getContentType())
                        .build();

                toolFile = toolFileRepository.save(toolFile);
                savedFiles.add(toDto(toolFile));

                log.info("Saved file: {} for tool {}", originalName, toolId);
            } catch (IOException e) {
                log.error("Failed to save file: {}", originalName, e);
                throw new FileValidationException("文件保存失败: " + originalName);
            }
        }

        // Save README if provided
        if (readme != null && !readme.isBlank()) {
            try {
                Path readmePath = toolFolder.resolve("readme.md");
                Files.writeString(readmePath, readme);

                ToolFile readmeFile = ToolFile.builder()
                        .toolId(toolId)
                        .originalName("readme.md")
                        .storedPath(Paths.get(String.valueOf(toolId), "readme.md").toString())
                        .fileSize((long) readme.getBytes().length)
                        .contentType("text/markdown")
                        .build();

                toolFileRepository.save(readmeFile);
                readmeSaved = true;

                log.info("Saved readme.md for tool {}", toolId);
            } catch (IOException e) {
                log.error("Failed to save readme for tool {}", toolId, e);
            }
        }

        return FileUploadResponse.builder()
                .toolId(toolId)
                .files(savedFiles)
                .readmeSaved(readmeSaved)
                .build();
    }

    @Transactional(readOnly = true)
    public FileListResponse getToolFiles(Long toolId) {
        List<ToolFile> files = toolFileRepository.findByToolIdAndStatusNormal(toolId);

        List<ToolFileDTO> fileDtos = files.stream()
                .map(this::toDto)
                .toList();

        // Check if readme exists
        Path readmePath = Paths.get(uploadConfig.getBaseDir(), String.valueOf(toolId), "readme.md");
        boolean readmeExists = Files.exists(readmePath);

        return FileListResponse.builder()
                .toolId(toolId)
                .folderPath(Paths.get(String.valueOf(toolId)).toString())
                .files(fileDtos)
                .readmeExists(readmeExists)
                .build();
    }

    @Transactional(readOnly = true)
    public ToolFile downloadFile(Long toolId, Long fileId) {
        ToolFile toolFile = toolFileRepository.findByIdAndToolId(fileId, toolId)
                .orElseThrow(() -> new ResourceNotFoundException("文件不存在"));

        // Verify file exists physically
        Path filePath = Paths.get(uploadConfig.getBaseDir(), toolFile.getStoredPath());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("文件不存在或已被删除");
        }

        return toolFile;
    }

    @Transactional(readOnly = true)
    public InputStream getFileInputStream(Long toolId, Long fileId) {
        ToolFile toolFile = downloadFile(toolId, fileId);
        Path filePath = Paths.get(uploadConfig.getBaseDir(), toolFile.getStoredPath());

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            throw new ResourceNotFoundException("文件读取失败");
        }
    }

    @Transactional
    public void deleteToolFile(Long toolId, Long fileId, Long userId) {
        // Verify the tool belongs to the user
        Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        if (!tool.getUploader().getId().equals(userId)) {
            throw new ForbiddenException("无权限删除此文件");
        }

        ToolFile toolFile = toolFileRepository.findByIdAndToolId(fileId, toolId)
                .orElseThrow(() -> new ResourceNotFoundException("文件不存在"));

        // Delete physical file
        Path filePath = Paths.get(uploadConfig.getBaseDir(), toolFile.getStoredPath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", filePath, e);
        }

        // Delete database record
        toolFileRepository.deleteById(fileId);

        log.info("Deleted file {} for tool {}", fileId, toolId);
    }

    @Transactional
    public void cleanupToolFiles(Long toolId) {
        List<ToolFile> files = toolFileRepository.findByToolId(toolId);

        // Delete physical files
        for (ToolFile file : files) {
            Path filePath = Paths.get(uploadConfig.getBaseDir(), file.getStoredPath());
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Failed to delete physical file: {}", filePath, e);
            }
        }

        // Delete tool folder
        Path toolFolder = Paths.get(uploadConfig.getBaseDir(), String.valueOf(toolId));
        try {
            Files.deleteIfExists(toolFolder);
        } catch (IOException e) {
            log.error("Failed to delete tool folder: {}", toolFolder, e);
        }

        // Delete database records
        toolFileRepository.deleteByToolId(toolId);

        log.info("Cleaned up all files for tool {}", toolId);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("文件大小超过限制 (50MB): " + file.getOriginalFilename());
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalName.isBlank()) {
            throw new FileValidationException("文件名无效");
        }

        String extension = getFileExtension(originalName).toLowerCase();
        List<String> allowedExtensions = uploadConfig.getAllowedExtensions();
        if (allowedExtensions != null && !allowedExtensions.isEmpty() && !allowedExtensions.contains(extension)) {
            throw new FileValidationException("不支持的文件类型: ." + extension);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    private ToolFileDTO toDto(ToolFile toolFile) {
        return ToolFileDTO.builder()
                .id(toolFile.getId())
                .toolId(toolFile.getToolId())
                .originalName(toolFile.getOriginalName())
                .storedPath(toolFile.getStoredPath())
                .fileSize(toolFile.getFileSize())
                .contentType(toolFile.getContentType())
                .createdAt(toolFile.getCreatedAt())
                .build();
    }
}
