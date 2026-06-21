package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToolService {

    private final ToolRepository toolRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ToolFileService toolFileService;

    @Transactional(readOnly = true)
    public PageResponse<ToolSummaryDTO> getTools(Long categoryId, String keyword, String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<Tool> toolPage;
        if ("name".equalsIgnoreCase(sortBy)) {
            toolPage = toolRepository.findByFiltersOrderByName(categoryId, keyword, pageable);
        } else {
            toolPage = toolRepository.findByFilters(categoryId, keyword, pageable);
        }

        return PageResponse.<ToolSummaryDTO>builder()
                .content(toolPage.getContent().stream().map(this::toSummaryDTO).toList())
                .totalElements(toolPage.getTotalElements())
                .totalPages(toolPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    @Transactional(readOnly = true)
    public ToolDetailDTO getToolById(Long id) {
        Tool tool = toolRepository.findByIdAndStatusNormal(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        return toDetailDTO(tool);
    }

    @Transactional
    public ToolSummaryDTO createTool(CreateToolRequest request, Long uploaderId) {
        // Check for duplicate tool name for this user in the same category
        if (toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                request.getName(), uploaderId, request.getCategoryId(), Tool.Status.NORMAL)) {
            throw new DuplicateResourceException("您已在该分类下上传过同名工具");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // Sanitize content for XSS
        String sanitizedContent = XssSanitizer.sanitize(request.getContent());

        Tool tool = Tool.builder()
                .name(request.getName())
                .category(category)
                .content(sanitizedContent)
                .version(request.getVersion())
                .uploader(uploader)
                .status(Tool.Status.NORMAL)
                .build();

        tool = toolRepository.save(tool);

        // Re-fetch with relations to avoid lazy init issues
        tool = toolRepository.findByIdAndStatusNormalWithRelations(tool.getId())
                .orElse(tool);

        return toSummaryDTO(tool);
    }

    @Transactional
    public ToolDetailDTO updateTool(Long id, UpdateToolRequest request, User user) {
        Tool tool = toolRepository.findByIdAndStatusNormal(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        boolean isOwner = tool.getUploader().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        String newName = request.getName() != null ? request.getName() : tool.getName();
        Long newCategoryId = request.getCategoryId() != null ? request.getCategoryId() : tool.getCategory().getId();

        // Check for duplicate name (excluding current tool, same category) - use original uploader's ID
        boolean nameChanged = !tool.getName().equals(newName);
        boolean categoryChanged = !tool.getCategory().getId().equals(newCategoryId);
        if (nameChanged || categoryChanged) {
            if (toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatusAndIdNot(
                    newName, tool.getUploader().getId(), newCategoryId, Tool.Status.NORMAL, id)) {
                throw new DuplicateResourceException("您已在该分类下上传过同名工具");
            }
        }

        if (categoryChanged) {
            Category category = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));
            tool.setCategory(category);
        }

        if (request.getContent() != null) {
            String sanitizedContent = XssSanitizer.sanitize(request.getContent());
            tool.setContent(sanitizedContent);
        }

        if (newName != null) {
            tool.setName(newName);
        }
        if (request.getVersion() != null && !request.getVersion().isBlank()) {
            tool.setVersion(request.getVersion());
        }

        tool = toolRepository.save(tool);
        return toDetailDTO(tool);
    }

    @Transactional
    public void deleteTool(Long id, User user) {
        Tool tool = toolRepository.findByIdAndStatusNormal(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        boolean isOwner = tool.getUploader().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        toolFileService.cleanupToolFiles(id);
        tool.setStatus(Tool.Status.DELETED);
        toolRepository.save(tool);
    }

    @Transactional
    public void incrementViewCount(Long toolId) {
        Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        tool.incrementViewCount();
        toolRepository.save(tool);
    }

    @Transactional(readOnly = true)
    public PageResponse<ToolSummaryDTO> getMyTools(Long uploaderId, Long categoryId, String keyword, String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<Tool> toolPage = toolRepository.findByUploaderIdAndFilters(
                uploaderId, categoryId, keyword, pageable);

        return PageResponse.<ToolSummaryDTO>builder()
                .content(toolPage.getContent().stream().map(this::toSummaryDTO).toList())
                .totalElements(toolPage.getTotalElements())
                .totalPages(toolPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private ToolSummaryDTO toSummaryDTO(Tool tool) {
        Hibernate.initialize(tool.getCategory());
        Hibernate.initialize(tool.getUploader());
        return ToolSummaryDTO.builder()
                .id(tool.getId())
                .name(tool.getName())
                .version(tool.getVersion())
                .categoryName(tool.getCategory().getName())
                .categoryIcon(tool.getCategory().getIcon())
                .uploaderId(tool.getUploader().getId())
                .uploaderUsername(tool.getUploader().getUsername())
                .uploaderNickname(tool.getUploader().getNickname())
                .createdAt(tool.getCreatedAt())
                .build();
    }

    private ToolDetailDTO toDetailDTO(Tool tool) {
        Hibernate.initialize(tool.getCategory());
        Hibernate.initialize(tool.getUploader());
        return ToolDetailDTO.builder()
                .id(tool.getId())
                .name(tool.getName())
                .version(tool.getVersion())
                .categoryName(tool.getCategory().getName())
                .categoryIcon(tool.getCategory().getIcon())
                .content(tool.getContent())
                .uploaderId(tool.getUploader().getId())
                .uploaderUsername(tool.getUploader().getUsername())
                .uploaderNickname(tool.getUploader().getNickname())
                .createdAt(tool.getCreatedAt())
                .updatedAt(tool.getUpdatedAt())
                .viewCount(tool.getViewCount() != null ? tool.getViewCount() : 0)
                .likeCount(tool.getLikeCount() != null ? tool.getLikeCount() : 0)
                .commentCount(tool.getCommentCount() != null ? tool.getCommentCount() : 0)
                .score(tool.getScore() != null ? tool.getScore() : java.math.BigDecimal.ZERO)
                .build();
    }
}
