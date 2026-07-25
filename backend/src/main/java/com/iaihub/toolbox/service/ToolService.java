package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.ToolTag;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.ToolTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolService {

    private final ToolRepository toolRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ToolFileService toolFileService;
    private final TagRepository tagRepository;
    private final ToolTagRepository toolTagRepository;
    private final UnifiedFavoriteRepository unifiedFavoriteRepository;
    private final ToolFileRepository toolFileRepository;

    @Transactional(readOnly = true)
    public PageResponse<ToolSummaryDTO> getTools(Long categoryId, String keyword, Long tagId, String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<Tool> toolPage;
        if (tagId != null) {
            if ("name".equalsIgnoreCase(sortBy)) {
                toolPage = toolRepository.findByFiltersWithTagOrderByName(categoryId, keyword, tagId, pageable);
            } else if ("latest".equalsIgnoreCase(sortBy)) {
                toolPage = toolRepository.findByFiltersWithTag(categoryId, keyword, tagId, pageable);
            } else {
                // 默认 hot：pinned DESC, score DESC
                toolPage = toolRepository.findByFiltersWithTagOrderByHot(categoryId, keyword, tagId, pageable);
            }
        } else if ("name".equalsIgnoreCase(sortBy)) {
            toolPage = toolRepository.findByFiltersOrderByName(categoryId, keyword, pageable);
        } else if ("latest".equalsIgnoreCase(sortBy)) {
            toolPage = toolRepository.findByFilters(categoryId, keyword, pageable);
        } else {
            // 默认 hot：pinned DESC, score DESC
            toolPage = toolRepository.findByFiltersOrderByHot(categoryId, keyword, pageable);
        }

        return PageResponse.<ToolSummaryDTO>builder()
                .content(toSummaryDTOList(toolPage.getContent()))
                .totalElements(toolPage.getTotalElements())
                .totalPages(toolPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    public void pinTool(Long id) {
        toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));
        toolRepository.pinById(id);
    }

    public void unpinTool(Long id) {
        toolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));
        toolRepository.unpinById(id);
    }

    public java.util.List<Long> getHotTop5() {
        return toolRepository.findTop5ByStatusOrderByScoreDesc(PageRequest.of(0, 5));
    }

    @Transactional
    public ToolDetailDTO getToolById(Long id) {
        Tool tool = toolRepository.findByIdAndStatusNormal(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        // 浏览量 +1（与论坛帖子、视频详情页保持一致）
        tool.incrementViewCount();
        toolRepository.save(tool);

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

        Tool tool = Tool.builder()
                .name(request.getName())
                .category(category)
                .content(request.getContent())
                .description(request.getDescription())
                .version(request.getVersion())
                .uploader(uploader)
                .status(Tool.Status.NORMAL)
                .build();

        tool = toolRepository.save(tool);

        // Handle tag associations
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            for (Long tagId : request.getTagIds()) {
                toolTagRepository.save(new ToolTag(tool.getId(), tagId));
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
        }

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
            tool.setContent(request.getContent());
        }

        if (request.getDescription() != null) {
            tool.setDescription(request.getDescription());
        }

        if (newName != null) {
            tool.setName(newName);
        }
        if (request.getVersion() != null && !request.getVersion().isBlank()) {
            tool.setVersion(request.getVersion());
        }

        // Handle tag replacement
        if (request.getTagIds() != null) {
            // Remove old tag associations and decrement usage
            List<ToolTag> oldTags = toolTagRepository.findByToolId(id);
            for (ToolTag tt : oldTags) {
                tagRepository.findById(tt.getTagId()).ifPresent(Tag::decrementUsage);
            }
            toolTagRepository.deleteByToolId(id);

            // Add new tag associations and increment usage
            for (Long tagId : request.getTagIds()) {
                toolTagRepository.save(new ToolTag(id, tagId));
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
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
    public void updateLogo(Long id, String logoUrl, User user) {
        Tool tool = toolRepository.findByIdAndStatusNormal(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));

        boolean isOwner = tool.getUploader().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        tool.setLogoUrl(logoUrl);
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
                .content(toSummaryDTOList(toolPage.getContent()))
                .totalElements(toolPage.getTotalElements())
                .totalPages(toolPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private List<ToolSummaryDTO> toSummaryDTOList(List<Tool> tools) {
        if (tools.isEmpty()) {
            return List.of();
        }
        List<Long> toolIds = tools.stream().map(Tool::getId).toList();
        Map<Long, Long> favoriteCounts = batchFavoriteCounts(toolIds);
        Map<Long, Long> downloadCounts = batchDownloadCounts(toolIds);
        return tools.stream()
                .map(tool -> toSummaryDTO(tool, favoriteCounts, downloadCounts))
                .toList();
    }

    private Map<Long, Long> batchFavoriteCounts(List<Long> toolIds) {
        return unifiedFavoriteRepository.countByTargetTypeAndTargetIdIn("TOOL", toolIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1], (a, b) -> a));
    }

    private Map<Long, Long> batchDownloadCounts(List<Long> toolIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : toolFileRepository.sumDownloadCountGroupByToolId(toolIds)) {
            result.put((Long) row[0], row[1] == null ? 0L : (Long) row[1]);
        }
        return result;
    }

    private String resolveLogoUrl(Tool tool) {
        if (tool.getLogoUrl() != null && !tool.getLogoUrl().isBlank()) {
            return tool.getLogoUrl();
        }
        // 分类默认 logo 由前端本地资源渲染，后端只返回工具自身 logo
        return null;
    }

    private long countFavorites(Long toolId) {
        return unifiedFavoriteRepository.countByTargetTypeAndTargetId("TOOL", toolId);
    }

    private long sumDownloads(Long toolId) {
        List<Object[]> rows = toolFileRepository.sumDownloadCountGroupByToolId(Collections.singletonList(toolId));
        if (rows.isEmpty() || rows.get(0)[1] == null) {
            return 0L;
        }
        return (Long) rows.get(0)[1];
    }

    private ToolSummaryDTO toSummaryDTO(Tool tool) {
        return toSummaryDTO(tool,
                Map.of(tool.getId(), countFavorites(tool.getId())),
                Map.of(tool.getId(), sumDownloads(tool.getId())));
    }

    private ToolSummaryDTO toSummaryDTO(Tool tool, Map<Long, Long> favoriteCounts, Map<Long, Long> downloadCounts) {
        Hibernate.initialize(tool.getCategory());
        Hibernate.initialize(tool.getUploader());
        List<TagDTO> tags = toolTagRepository.findByToolId(tool.getId()).stream()
                .map(tt -> tagRepository.findById(tt.getTagId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(t -> new TagDTO(t.getId(), t.getName(), t.getTagType().name(), t.getUsageCount()))
                .toList();
        return ToolSummaryDTO.builder()
                .id(tool.getId())
                .name(tool.getName())
                .version(tool.getVersion())
                .description(tool.getDescription())
                .categoryName(tool.getCategory().getName())
                .categoryIcon(tool.getCategory().getIcon())
                .logoUrl(resolveLogoUrl(tool))
                .uploaderId(tool.getUploader().getId())
                .uploaderUsername(tool.getUploader().getUsername())
                .uploaderNickname(tool.getUploader().getNickname())
                .createdAt(tool.getCreatedAt())
                .score(tool.getScore() != null ? tool.getScore() : java.math.BigDecimal.ZERO)
                .pinned(tool.getPinned() != null ? tool.getPinned() : false)
                .viewCount(tool.getViewCount() != null ? tool.getViewCount() : 0)
                .likeCount(tool.getLikeCount() != null ? tool.getLikeCount() : 0)
                .commentCount(tool.getCommentCount() != null ? tool.getCommentCount() : 0)
                .favoriteCount(favoriteCounts.getOrDefault(tool.getId(), 0L).intValue())
                .downloadCount(downloadCounts.getOrDefault(tool.getId(), 0L).intValue())
                .tags(tags)
                .build();
    }

    private ToolDetailDTO toDetailDTO(Tool tool) {
        Hibernate.initialize(tool.getCategory());
        Hibernate.initialize(tool.getUploader());
        List<TagDTO> tags = toolTagRepository.findByToolId(tool.getId()).stream()
                .map(tt -> tagRepository.findById(tt.getTagId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(t -> new TagDTO(t.getId(), t.getName(), t.getTagType().name(), t.getUsageCount()))
                .toList();
        return ToolDetailDTO.builder()
                .id(tool.getId())
                .name(tool.getName())
                .version(tool.getVersion())
                .description(tool.getDescription())
                .categoryName(tool.getCategory().getName())
                .categoryIcon(tool.getCategory().getIcon())
                .logoUrl(resolveLogoUrl(tool))
                .content(tool.getContent())
                .uploaderId(tool.getUploader().getId())
                .uploaderUsername(tool.getUploader().getUsername())
                .uploaderNickname(tool.getUploader().getNickname())
                .createdAt(tool.getCreatedAt())
                .updatedAt(tool.getUpdatedAt())
                .viewCount(tool.getViewCount() != null ? tool.getViewCount() : 0)
                .likeCount(tool.getLikeCount() != null ? tool.getLikeCount() : 0)
                .commentCount(tool.getCommentCount() != null ? tool.getCommentCount() : 0)
                .favoriteCount((int) countFavorites(tool.getId()))
                .downloadCount((int) sumDownloads(tool.getId()))
                .score(tool.getScore() != null ? tool.getScore() : java.math.BigDecimal.ZERO)
                .tags(tags)
                .build();
    }
}
