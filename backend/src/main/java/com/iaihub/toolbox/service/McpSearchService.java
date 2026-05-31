package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.PostSearchResult;
import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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

    public McpSearchService(ToolRepository toolRepository,
                            ToolFileRepository toolFileRepository,
                            ForumPostRepository forumPostRepository,
                            UserRepository userRepository) {
        this.toolRepository = toolRepository;
        this.toolFileRepository = toolFileRepository;
        this.forumPostRepository = forumPostRepository;
        this.userRepository = userRepository;
    }

    /**
     * 搜索工具
     */
    @Transactional(readOnly = true)
    public List<ToolSearchResult> searchTools(String query, String category, Integer limit) {
        logger.info("Searching tools: query={}, category={}, limit={}", query, category, limit);

        int limitValue = limit != null ? limit : 20;

        List<Tool> tools = toolRepository.findApprovedToolsWithCategory(query, PageRequest.of(0, limitValue));
        return tools.stream()
                .map(tool -> new ToolSearchResult(
                        tool.getId(),
                        tool.getName(),
                        tool.getContent() != null ? tool.getContent().substring(0, Math.min(100, tool.getContent().length())) : "",
                        tool.getCategory() != null ? tool.getCategory().getName() : "",
                        tool.getCreatedAt() != null ? tool.getCreatedAt().toString() : ""
                ))
                .collect(Collectors.toList());
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
            posts = forumPostRepository.searchByTitle(query, ForumPostStatus.NORMAL, pageable).getContent();
        } else {
            posts = forumPostRepository.findByStatusOrderByCreatedAtDesc(ForumPostStatus.NORMAL, pageable).getContent();
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
}