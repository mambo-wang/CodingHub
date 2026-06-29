package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.PostSearchResult;
import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.forum.ForumPostVisibility;
import com.iaihub.toolbox.model.tag.ToolTag;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.ToolTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

/**
 * MCP 搜索服务
 * 封装工具和帖子检索
 */
@Service
public class McpSearchService {

    private static final Logger logger = LoggerFactory.getLogger(McpSearchService.class);

    private final ToolRepository toolRepository;
    private final ToolFileRepository toolFileRepository;
    private final ForumPostRepository forumPostRepository;
    private final UserRepository userRepository;
    private final ToolTagRepository toolTagRepository;
    private final TagRepository tagRepository;

    public McpSearchService(ToolRepository toolRepository,
                            ToolFileRepository toolFileRepository,
                            ForumPostRepository forumPostRepository,
                            UserRepository userRepository,
                            ToolTagRepository toolTagRepository,
                            TagRepository tagRepository) {
        this.toolRepository = toolRepository;
        this.toolFileRepository = toolFileRepository;
        this.forumPostRepository = forumPostRepository;
        this.userRepository = userRepository;
        this.toolTagRepository = toolTagRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * 搜索工具
     */
    @Transactional(readOnly = true)
    public List<ToolSearchResult> searchTools(String query, String category, Integer limit) {
        logger.info("Searching tools: query={}, category={}, limit={}", query, category, limit);

        int limitValue = limit != null ? limit : 20;

        List<Tool> tools = toolRepository.findApprovedToolsWithCategory(query, PageRequest.of(0, limitValue));

        // Batch-fetch tags for all tools to avoid N+1 queries
        Map<Long, List<TagDTO>> tagsMap = resolveTagsForTools(tools);

        return tools.stream()
                .map(tool -> {
                    ToolSearchResult result = new ToolSearchResult(
                            tool.getId(),
                            tool.getName(),
                            tool.getContent() != null ? tool.getContent().substring(0, Math.min(100, tool.getContent().length())) : "",
                            tool.getCategory() != null ? tool.getCategory().getName() : "",
                            tool.getVersion() != null ? tool.getVersion() : "1.0.0",
                            tool.getCreatedAt() != null ? tool.getCreatedAt().toString() : ""
                    );
                    result.setTags(tagsMap.getOrDefault(tool.getId(), Collections.emptyList()));
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取单个工具的标签列表
     */
    public List<TagDTO> getToolTags(Long toolId) {
        List<ToolTag> toolTags = toolTagRepository.findByToolId(toolId);
        if (toolTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = toolTags.stream().map(ToolTag::getTagId).collect(Collectors.toList());
        return tagRepository.findAllById(tagIds).stream()
                .map(tag -> new TagDTO(tag.getId(), tag.getName(), tag.getTagType().name(), tag.getUsageCount()))
                .collect(Collectors.toList());
    }

    /**
     * 批量解析工具标签（避免 N+1 查询）
     */
    private Map<Long, List<TagDTO>> resolveTagsForTools(List<Tool> tools) {
        if (tools.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> toolIds = tools.stream().map(Tool::getId).collect(Collectors.toList());

        // Collect all tool-tag associations
        Map<Long, List<Long>> toolTagIds = new HashMap<>();
        Set<Long> allTagIds = new HashSet<>();
        for (Long toolId : toolIds) {
            List<ToolTag> tts = toolTagRepository.findByToolId(toolId);
            List<Long> tids = tts.stream().map(ToolTag::getTagId).collect(Collectors.toList());
            toolTagIds.put(toolId, tids);
            allTagIds.addAll(tids);
        }

        // Batch-fetch all tags
        Map<Long, TagDTO> tagDtoMap = new HashMap<>();
        if (!allTagIds.isEmpty()) {
            tagRepository.findAllById(allTagIds).forEach(tag ->
                    tagDtoMap.put(tag.getId(), new TagDTO(tag.getId(), tag.getName(), tag.getTagType().name(), tag.getUsageCount()))
            );
        }

        // Map tags to tools
        Map<Long, List<TagDTO>> result = new HashMap<>();
        for (Long toolId : toolIds) {
            List<Long> tids = toolTagIds.getOrDefault(toolId, Collections.emptyList());
            List<TagDTO> tags = tids.stream()
                    .map(tagDtoMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            result.put(toolId, tags);
        }
        return result;
    }

    /**
     * 获取工具详情
     */
    public Tool getToolById(Long toolId) {
        logger.info("Getting tool by id: {}", toolId);
        return toolRepository.findByIdAndStatusNormalWithRelations(toolId).orElse(null);
    }

    /**
     * 获取工具文件列表
     */
    public List<ToolFile> getToolFiles(Long toolId) {
        logger.info("Getting tool files: toolId={}", toolId);
        return toolFileRepository.findByToolIdAndStatusNormal(toolId);
    }

    /**
     * 搜索帖子
     */
    public List<PostSearchResult> searchPosts(String query, Integer limit) {
        logger.info("Searching posts: query={}, limit={}", query, limit);

        int limitValue = limit != null ? limit : 20;
        PageRequest pageable = PageRequest.of(0, limitValue);

        List<ForumPost> posts;
        if (query != null && !query.isEmpty()) {
            posts = forumPostRepository.searchByTitle(query, ForumPostStatus.NORMAL, ForumPostVisibility.PUBLIC, pageable).getContent();
        } else {
            posts = forumPostRepository.findByStatusAndVisibilityOrderByCreatedAtDesc(ForumPostStatus.NORMAL, ForumPostVisibility.PUBLIC, pageable).getContent();
        }

        return posts.stream()
                .map(post -> {
                    String authorName = userRepository.findById(post.getAuthorId())
                            .map(u -> u.getUsername())
                            .orElse("unknown");
                    String summary = post.getContent() != null && post.getContent().length() > 100
                            ? post.getContent().substring(0, 100) + "..."
                            : post.getContent();
                    return new PostSearchResult(
                            post.getId(),
                            post.getTitle(),
                            summary,
                            authorName,
                            post.getCreatedAt() != null ? post.getCreatedAt().toString() : ""
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取帖子详情
     */
    public ForumPost getPostById(Long postId) {
        logger.info("Getting post by id: {}", postId);
        return forumPostRepository.findById(postId).orElse(null);
    }

    /**
     * 获取工具文件详情
     */
    public com.iaihub.toolbox.model.ToolFile getToolFile(Long toolId, Long fileId) {
        logger.info("Getting tool file: toolId={}, fileId={}", toolId, fileId);
        return toolFileRepository.findByIdAndToolId(fileId, toolId).orElse(null);
    }
}