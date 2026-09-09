package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.FileValidationException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.forum.*;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ForumPostService {

    /** 导入允许的扩展名（Q4/Q8：服务端 multipart 导入，解析后即丢弃文件）。 */
    private static final List<String> ALLOWED_IMPORT_EXTENSIONS = List.of("md", "markdown", "html", "htm");
    private static final long MAX_IMPORT_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_TITLE_LENGTH = 200;

    private static final Pattern HTML_TITLE = Pattern.compile("<title[^>]*>(.*?)</title>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_HEADING = Pattern.compile("<h1[^>]*>(.*?)</h1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern MD_HEADING = Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE);

    private final ForumPostRepository postRepository;
    private final ForumCategoryRepository categoryRepository;
    private final ForumPostTagRepository postTagRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    public Page<ForumPostDTO> getPostList(Long categoryId, Long tagId, String keyword, String sortBy, Pageable pageable) {
        Page<ForumPost> posts;
        ForumPostVisibility visibility = ForumPostVisibility.PUBLIC;

        boolean latest = "latest".equalsIgnoreCase(sortBy);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (tagId != null) {
            if (latest) {
                posts = hasKeyword
                    ? postRepository.searchByTagIdAndTitle(tagId, keyword, ForumPostStatus.NORMAL, visibility, pageable)
                    : postRepository.findByTagIdAndStatusAndVisibilityOrderByCreatedAtDesc(tagId, ForumPostStatus.NORMAL, visibility, pageable);
            } else {
                posts = hasKeyword
                    ? postRepository.searchByTagIdAndTitleOrderByHot(tagId, keyword, ForumPostStatus.NORMAL, visibility, pageable)
                    : postRepository.findByTagIdAndStatusAndVisibilityOrderByHot(tagId, ForumPostStatus.NORMAL, visibility, pageable);
            }
        } else if (latest) {
            if (hasKeyword) {
                posts = postRepository.searchByTitle(keyword, ForumPostStatus.NORMAL, visibility, pageable);
            } else if (categoryId != null) {
                posts = postRepository.findByCategoryIdAndStatusAndVisibility(categoryId, ForumPostStatus.NORMAL, visibility, pageable);
            } else {
                posts = postRepository.findByStatusAndVisibilityOrderByCreatedAtDesc(ForumPostStatus.NORMAL, visibility, pageable);
            }
        } else {
            if (hasKeyword) {
                posts = postRepository.searchByTitleOrderByHot(keyword, ForumPostStatus.NORMAL, visibility, pageable);
            } else if (categoryId != null) {
                posts = postRepository.findByCategoryIdAndStatusAndVisibilityOrderByHot(categoryId, ForumPostStatus.NORMAL, visibility, pageable);
            } else {
                posts = postRepository.findByStatusAndVisibilityOrderByHot(ForumPostStatus.NORMAL, visibility, pageable);
            }
        }

        return posts.map(this::toDTO);
    }

    public void pinPost(Long id) {
        postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));
        postRepository.pinById(id);
    }

    public void unpinPost(Long id) {
        postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));
        postRepository.unpinById(id);
    }

    public List<Long> getHotTop5() {
        return postRepository.findTop5ByStatusOrderByScoreDesc(PageRequest.of(0, 5));
    }

    public Page<ForumPostDTO> getMyPosts(Long userId, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(userId, ForumPostStatus.NORMAL, pageable);
        return posts.map(this::toDTO);
    }

    public ForumPostDTO getPostById(Long id, User currentUser) {
        findAccessiblePost(id, currentUser);

        // 浏览量原子 +1（不触发 @PreUpdate，避免 updatedAt 被刷新为当前时间）
        postRepository.incrementViewCount(id);

        // 重新读取，返回最新浏览量
        ForumPost fresh = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));

        return toDTO(fresh);
    }

    /**
     * 读取帖子用于编辑（MCP post_update 需要先取原值再合并字段）。
     * 与 {@link #getPostById} 的唯一区别是<b>不自增浏览量</b>。
     */
    public ForumPostDTO getPostForEdit(Long id, User currentUser) {
        return toDTO(findAccessiblePost(id, currentUser));
    }

    private ForumPost findAccessiblePost(Long id, User currentUser) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));

        // Private posts: only author and admin can view
        if (post.getVisibility() == ForumPostVisibility.PRIVATE) {
            if (currentUser == null) {
                throw new ForbiddenException("该帖子为私有帖子，请登录后查看");
            }
            boolean isOwner = post.getAuthorId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;
            if (!isOwner && !isAdmin) {
                throw new ForbiddenException("该帖子为私有帖子，无权查看");
            }
        }

        return post;
    }

    @Transactional
    public ForumPostDTO createPost(Long authorId, ForumPostCreateRequest request) {
        ForumPost post = new ForumPost();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setContentFormat(parseFormat(request.contentFormat()));
        post.setAuthorId(authorId);
        post.setCategoryId(request.categoryId());
        post.setStatus(ForumPostStatus.NORMAL);

        // Set visibility from request, default to PUBLIC
        if (request.visibility() != null && !request.visibility().isBlank()) {
            post.setVisibility(ForumPostVisibility.valueOf(request.visibility()));
        } else {
            post.setVisibility(ForumPostVisibility.PUBLIC);
        }

        post = postRepository.save(post);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            for (Long tagId : request.tagIds()) {
                ForumPostTag pt = new ForumPostTag();
                pt.setPostId(post.getId());
                pt.setTagId(tagId);
                postTagRepository.save(pt);
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
        }

        return toDTO(post);
    }

    @Transactional
    public ForumPostDTO updatePost(Long postId, User user, ForumPostCreateRequest request) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        boolean isOwner = post.getAuthorId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        post.setTitle(request.title());
        post.setContent(request.content());
        post.setContentFormat(parseFormat(request.contentFormat()));
        post.setCategoryId(request.categoryId());

        // Update visibility if provided
        if (request.visibility() != null && !request.visibility().isBlank()) {
            post.setVisibility(ForumPostVisibility.valueOf(request.visibility()));
        }

        // Handle tag replacement
        if (request.tagIds() != null) {
            // Remove old tag associations and decrement usage
            List<ForumPostTag> oldTags = postTagRepository.findByPostId(postId);
            for (ForumPostTag pt : oldTags) {
                tagRepository.findById(pt.getTagId()).ifPresent(Tag::decrementUsage);
            }
            postTagRepository.deleteByPostId(postId);

            // Add new tag associations and increment usage
            for (Long tagId : request.tagIds()) {
                ForumPostTag pt = new ForumPostTag();
                pt.setPostId(postId);
                pt.setTagId(tagId);
                postTagRepository.save(pt);
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
        }

        post = postRepository.save(post);
        return toDTO(post);
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));

        boolean isOwner = post.getAuthorId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        post.setStatus(ForumPostStatus.DELETED);
        postRepository.save(post);
    }

    /**
     * 导入 .md / .html 文件并直接建帖。文件仅用于解析正文，解析完即丢弃，不落盘留存。
     *
     * <p>title / categoryId / visibility / contentFormat 均可由调用方显式覆盖；
     * 缺省时按 Q8 的规则推导：标题取 &lt;title&gt;、&lt;h1&gt;、Markdown 首个标题，退化为文件名；
     * 格式按扩展名判定；分类取排序最靠前的分类。
     */
    @Transactional
    public ForumPostDTO importPost(Long authorId, MultipartFile file, String titleOverride,
                                   Long categoryId, String visibility, String contentFormatOverride) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("请选择要导入的 .md 或 .html 文件");
        }
        if (file.getSize() > MAX_IMPORT_SIZE) {
            throw new FileValidationException("导入文件大小不能超过 10MB");
        }

        String filename = file.getOriginalFilename();
        String extension = extensionOf(filename);
        if (!ALLOWED_IMPORT_EXTENSIONS.contains(extension)) {
            throw new FileValidationException("不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_IMPORT_EXTENSIONS));
        }

        String raw = readAsUtf8(file);
        ContentFormat format = contentFormatOverride != null && !contentFormatOverride.isBlank()
                ? parseFormat(contentFormatOverride)
                : detectFormat(extension);

        String title = titleOverride != null && !titleOverride.isBlank()
                ? titleOverride.trim()
                : deriveTitle(raw, format, filename);

        Long resolvedCategoryId = categoryId != null
                ? categoryId
                : categoryRepository.findAllByOrderBySortOrderAsc().stream()
                        .findFirst()
                        .map(ForumCategory::getId)
                        .orElseThrow(() -> new BusinessException("未提供分类，且系统暂无可用分类"));

        ForumPostCreateRequest request = new ForumPostCreateRequest(
                title, raw, resolvedCategoryId, null, visibility, format.name());

        return createPost(authorId, request);
    }

    private ContentFormat parseFormat(String value) {
        if (value == null || value.isBlank()) {
            return ContentFormat.MARKDOWN;
        }
        try {
            return ContentFormat.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的正文格式: " + value + "，可选 MARKDOWN 或 HTML");
        }
    }

    private ContentFormat detectFormat(String extension) {
        return "html".equals(extension) || "htm".equals(extension) ? ContentFormat.HTML : ContentFormat.MARKDOWN;
    }

    private String readAsUtf8(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            // 剥离 UTF-8 BOM：Windows 记事本保存的 .md 普遍带 BOM，
            // 不剥掉会让 Markdown 标题推导（^# 锚在行首）失效，也会污染正文。
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            return text;
        } catch (IOException e) {
            throw new FileValidationException("读取导入文件失败，请确认文件编码为 UTF-8");
        }
    }

    private String deriveTitle(String raw, ContentFormat format, String filename) {
        String extracted = format == ContentFormat.HTML ? deriveTitleFromHtml(raw) : deriveTitleFromMarkdown(raw);
        if (extracted != null && !extracted.isBlank()) {
            return extracted.length() > MAX_TITLE_LENGTH ? extracted.substring(0, MAX_TITLE_LENGTH) : extracted;
        }
        return stripExtension(filename);
    }

    private String deriveTitleFromHtml(String raw) {
        String fromTitle = firstMatch(HTML_TITLE, raw);
        return fromTitle != null ? fromTitle : firstMatch(HTML_HEADING, raw);
    }

    private String deriveTitleFromMarkdown(String raw) {
        return firstMatch(MD_HEADING, raw);
    }

    private String firstMatch(Pattern pattern, String raw) {
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        return stripInlineTags(matcher.group(1));
    }

    /** 去掉内联 HTML 标签并压缩空白——标题里不需要标签，也不该有换行。 */
    private String stripInlineTags(String value) {
        return value.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String stripExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "未命名帖子";
        }
        String name = filename;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.isBlank() ? "未命名帖子" : name;
    }

    private ForumPostDTO toDTO(ForumPost post) {
        String categoryName = categoryRepository.findById(post.getCategoryId())
            .map(ForumCategory::getName).orElse("未分类");

        String authorName = userRepository.findById(post.getAuthorId())
            .map(u -> u.getUsername())
            .orElse("用户" + post.getAuthorId());

        String authorNickname = userRepository.findById(post.getAuthorId())
            .map(u -> u.getNickname())
            .orElse(null);

        List<TagDTO> tags = postTagRepository.findByPostId(post.getId()).stream()
            .map(pt -> tagRepository.findById(pt.getTagId()).orElse(null))
            .filter(java.util.Objects::nonNull)
            .map(t -> new TagDTO(t.getId(), t.getName(), t.getTagType().name(), t.getUsageCount()))
            .toList();

        return new ForumPostDTO(
            post.getId(), post.getTitle(), post.getContent(),
            post.getContentFormat() != null ? post.getContentFormat().name() : ContentFormat.MARKDOWN.name(),
            post.getAuthorId(), authorName,
            authorNickname,
            post.getCategoryId(), categoryName,
            post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
            post.getCreatedAt(), post.getUpdatedAt(),
            post.getScore() != null ? post.getScore() : java.math.BigDecimal.ZERO,
            post.getPinned() != null ? post.getPinned() : false,
            post.getVisibility() != null ? post.getVisibility().name() : "PUBLIC",
            tags
        );
    }
}