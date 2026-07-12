package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.*;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedCommentRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedCommentServiceTest {

    @Mock
    private UnifiedCommentRepository commentRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ForumPostRepository forumPostRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    private UnifiedCommentService commentService;

    private Tool testTool;
    private User testUser;

    @BeforeEach
    void setUp() {
        commentService = new UnifiedCommentService(
                commentRepository, toolRepository, forumPostRepository, videoRepository, userRepository);

        testTool = Tool.builder()
                .id(1L).name("Test Tool")
                .commentCount(2).status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(100L).username("testuser").nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== addComment: top-level ====================

    @Test
    void addComment_topLevel_loggedInUser() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> {
            UnifiedComment c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        InteractionResponse response = commentService.addComment(
                "TOOL", 1L, 100L, null, "Great tool!", null);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertNull(response.getParentId());
        assertNull(response.getRootId());
        assertEquals("Test User", response.getUserNickname());
        assertEquals(3, testTool.getCommentCount());
        verify(commentRepository).save(any(UnifiedComment.class));
    }

    @Test
    void addComment_topLevel_anonymousUser() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> {
            UnifiedComment c = inv.getArgument(0);
            c.setId(2L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        InteractionResponse response = commentService.addComment(
                "TOOL", 1L, null, "匿名", "Anonymous comment", null);

        assertNotNull(response);
        assertNull(response.getUserId());
        assertEquals("匿名", response.getUserName());
    }

    // ==================== addComment: nested reply ====================

    @Test
    void addComment_nestedReply() {
        UnifiedComment parentComment = UnifiedComment.builder()
                .id(1L).targetType("TOOL").targetId(1L)
                .userId(100L).content("Parent comment")
                .createdAt(LocalDateTime.now()).build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> {
            UnifiedComment c = inv.getArgument(0);
            c.setId(3L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        InteractionResponse response = commentService.addComment(
                "TOOL", 1L, 100L, null, "Reply to parent", 1L);

        assertNotNull(response);
        assertEquals(1L, response.getParentId());
        assertEquals(1L, response.getRootId()); // rootId = parent.id (since parent has no rootId)
    }

    @Test
    void addComment_nestedReply_deepNesting() {
        // Parent is itself a reply (has rootId set)
        UnifiedComment parentReply = UnifiedComment.builder()
                .id(5L).targetType("TOOL").targetId(1L)
                .userId(100L).parentId(1L).rootId(1L)
                .content("Nested reply").build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(commentRepository.findById(5L)).thenReturn(Optional.of(parentReply));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response = commentService.addComment(
                "TOOL", 1L, 100L, null, "Deep reply", 5L);

        assertEquals(5L, response.getParentId());
        assertEquals(1L, response.getRootId()); // inherits parent's rootId
    }

    // ==================== addComment: XSS filter ====================

    @Test
    void addComment_xssFiltered() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> {
            UnifiedComment c = inv.getArgument(0);
            c.setId(4L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        InteractionResponse response = commentService.addComment(
                "TOOL", 1L, 100L, null, "<script>alert('xss')</script>", null);

        assertFalse(response.getContent().contains("<script>"), "XSS should be escaped");
        assertTrue(response.getContent().contains("&lt;script&gt;"));
    }

    // ==================== addComment: empty content ====================

    @Test
    void addComment_emptyContent() {
        assertThrows(BusinessException.class, () ->
                commentService.addComment("TOOL", 1L, 100L, null, "", null));
    }

    @Test
    void addComment_blankContent() {
        assertThrows(BusinessException.class, () ->
                commentService.addComment("TOOL", 1L, 100L, null, "   ", null));
    }

    // ==================== addComment: resource not found ====================

    @Test
    void addComment_toolNotFound() {
        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                commentService.addComment("TOOL", 999L, 100L, null, "Hello", null));
    }

    @Test
    void addComment_forumPostNotFound() {
        when(forumPostRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                commentService.addComment("FORUM_POST", 999L, 100L, null, "Hello", null));
    }

    @Test
    void addComment_videoNotFound() {
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                commentService.addComment("VIDEO", 999L, 100L, null, "Hello", null));
    }

    // ==================== getComments ====================

    @Test
    void getComments_paginated() {
        UnifiedComment c1 = UnifiedComment.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(100L)
                .content("First comment").createdAt(LocalDateTime.now()).build();
        UnifiedComment c2 = UnifiedComment.builder()
                .id(2L).targetType("TOOL").targetId(1L).userId(100L)
                .content("Second comment").createdAt(LocalDateTime.now()).build();

        Page<UnifiedComment> page = new PageImpl<>(List.of(c1, c2));
        when(commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(eq("TOOL"), eq(1L), any(Pageable.class)))
                .thenReturn(page);
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        PageResponse<InteractionResponse> response = commentService.getComments("TOOL", 1L, 0, 20);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Test User", response.getContent().get(0).getUserNickname());
    }

    // ==================== getMyComments ====================

    @Test
    void getMyComments_loggedInUser_tool_resolvesTargetTitle() {
        UnifiedComment c = UnifiedComment.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(100L)
                .content("Great tool!").createdAt(LocalDateTime.now()).build();
        Page<UnifiedComment> page = new PageImpl<>(List.of(c));
        when(commentRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(page);
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        PageResponse<UnifiedCommentService.MyCommentDTO> response = commentService.getMyComments(100L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        UnifiedCommentService.MyCommentDTO dto = response.getContent().get(0);
        assertEquals("TOOL", dto.getTargetType());
        assertEquals(1L, dto.getTargetId());
        assertEquals("Test Tool", dto.getTargetTitle());
        assertEquals("Great tool!", dto.getContent());
    }

    @Test
    void getMyComments_forumPost_resolvesTargetTitle() {
        ForumPost post = ForumPost.builder()
                .id(10L).title("My Post").commentCount(0)
                .status(ForumPostStatus.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        UnifiedComment c = UnifiedComment.builder()
                .id(2L).targetType("FORUM_POST").targetId(10L).userId(100L)
                .content("Nice post!").createdAt(LocalDateTime.now()).build();
        Page<UnifiedComment> page = new PageImpl<>(List.of(c));
        when(commentRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(page);
        when(forumPostRepository.findById(10L)).thenReturn(Optional.of(post));

        PageResponse<UnifiedCommentService.MyCommentDTO> response = commentService.getMyComments(100L, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals("My Post", response.getContent().get(0).getTargetTitle());
    }

    @Test
    void getMyComments_video_resolvesTargetTitle() {
        Video video = Video.builder()
                .id(20L).title("My Video").commentCount(0)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        UnifiedComment c = UnifiedComment.builder()
                .id(3L).targetType("VIDEO").targetId(20L).userId(100L)
                .content("Great video!").createdAt(LocalDateTime.now()).build();
        Page<UnifiedComment> page = new PageImpl<>(List.of(c));
        when(commentRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(page);
        when(videoRepository.findByIdAndStatus(20L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));

        PageResponse<UnifiedCommentService.MyCommentDTO> response = commentService.getMyComments(100L, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals("My Video", response.getContent().get(0).getTargetTitle());
    }

    @Test
    void getMyComments_skipsDeletedTarget() {
        UnifiedComment c = UnifiedComment.builder()
                .id(4L).targetType("TOOL").targetId(999L).userId(100L)
                .content("Orphan comment").createdAt(LocalDateTime.now()).build();
        Page<UnifiedComment> page = new PageImpl<>(List.of(c));
        when(commentRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(page);
        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        PageResponse<UnifiedCommentService.MyCommentDTO> response = commentService.getMyComments(100L, 0, 10);

        assertEquals(0, response.getContent().size());
    }

    @Test
    void getMyComments_anonymousUser_throws401() {
        assertThrows(BusinessException.class, () ->
                commentService.getMyComments(null, 0, 10));
    }

    // ==================== deleteComment ====================

    @Test
    void deleteComment_owner() {
        UnifiedComment comment = UnifiedComment.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(100L)
                .content("My comment").build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        assertDoesNotThrow(() -> commentService.deleteComment(1L, 100L, false));
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_admin() {
        UnifiedComment comment = UnifiedComment.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(200L)
                .content("Someone else's comment").build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        assertDoesNotThrow(() -> commentService.deleteComment(1L, 100L, true));
    }

    @Test
    void deleteComment_forbidden() {
        UnifiedComment comment = UnifiedComment.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(200L)
                .content("Someone else's comment").build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class, () ->
                commentService.deleteComment(1L, 100L, false));
    }

    @Test
    void deleteComment_notFound() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                commentService.deleteComment(999L, 100L, false));
    }

    // ==================== addComment: forum post & video ====================

    @Test
    void addComment_forumPost() {
        ForumPost post = ForumPost.builder()
                .id(10L).title("Post").commentCount(0)
                .status(ForumPostStatus.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(forumPostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> {
            UnifiedComment c = inv.getArgument(0);
            c.setId(10L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        InteractionResponse response = commentService.addComment(
                "FORUM_POST", 10L, 100L, null, "Nice post!", null);

        assertNotNull(response);
        assertEquals(1, post.getCommentCount());
    }

    @Test
    void addComment_video() {
        Video video = Video.builder()
                .id(20L).title("Video").commentCount(0)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(videoRepository.findByIdAndStatus(20L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(UnifiedComment.class))).thenAnswer(inv -> {
            UnifiedComment c = inv.getArgument(0);
            c.setId(20L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        InteractionResponse response = commentService.addComment(
                "VIDEO", 20L, 100L, null, "Great video!", null);

        assertNotNull(response);
        assertEquals(1, video.getCommentCount());
    }
}
