package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.forum.ForumCategory;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.repository.forum.ForumCategoryRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.forum.ForumPostTagRepository;
import com.iaihub.toolbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumPostServiceTest {

    @Mock
    private ForumPostRepository postRepository;
    @Mock
    private ForumCategoryRepository categoryRepository;
    @Mock
    private ForumPostTagRepository postTagRepository;
    @Mock
    private UserRepository userRepository;

    private ForumPostService forumPostService;

    private User owner;
    private User adminUser;
    private User regularUser;
    private ForumPost testPost;
    private ForumCategory testCategory;

    @BeforeEach
    void setUp() {
        forumPostService = new ForumPostService(
                postRepository, categoryRepository, postTagRepository, userRepository
        );

        owner = User.builder().id(1L).username("author").role(Role.USER).build();
        adminUser = User.builder().id(2L).username("admin").role(Role.ADMIN).build();
        regularUser = User.builder().id(3L).username("regular").role(Role.USER).build();

        testCategory = ForumCategory.builder().id(1L).name("技术交流").build();

        testPost = ForumPost.builder()
                .id(1L)
                .title("测试帖子")
                .content("测试内容")
                .authorId(1L)
                .categoryId(1L)
                .status(ForumPostStatus.NORMAL)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ===== deletePost tests =====

    @Test
    void deletePost_ownerCanDelete() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertDoesNotThrow(() -> forumPostService.deletePost(1L, owner));
        assertEquals(ForumPostStatus.DELETED, testPost.getStatus());
        verify(postRepository).save(testPost);
    }

    @Test
    void deletePost_adminCanDelete() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertDoesNotThrow(() -> forumPostService.deletePost(1L, adminUser));
        assertEquals(ForumPostStatus.DELETED, testPost.getStatus());
    }

    @Test
    void deletePost_regularUserThrows403() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertThrows(ForbiddenException.class, () ->
                forumPostService.deletePost(1L, regularUser));
    }

    @Test
    void deletePost_notFoundThrows404() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                forumPostService.deletePost(999L, owner));
    }

    // ===== updatePost tests =====

    @Test
    void updatePost_ownerCanUpdate() {
        ForumPostCreateRequest request = new ForumPostCreateRequest("更新标题", "更新内容", 1L, null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(ForumPost.class))).thenReturn(testPost);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ForumPostDTO result = assertDoesNotThrow(() ->
                forumPostService.updatePost(1L, owner, request));
        assertNotNull(result);
        assertEquals("更新标题", testPost.getTitle());
    }

    @Test
    void updatePost_adminCanUpdate() {
        ForumPostCreateRequest request = new ForumPostCreateRequest("管理员更新", "管理员内容", 1L, null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(ForumPost.class))).thenReturn(testPost);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ForumPostDTO result = assertDoesNotThrow(() ->
                forumPostService.updatePost(1L, adminUser, request));
        assertNotNull(result);
    }

    @Test
    void updatePost_regularUserThrows403() {
        ForumPostCreateRequest request = new ForumPostCreateRequest("尝试修改", "内容", 1L, null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertThrows(ForbiddenException.class, () ->
                forumPostService.updatePost(1L, regularUser, request));
    }

    @Test
    void updatePost_notFoundThrows404() {
        ForumPostCreateRequest request = new ForumPostCreateRequest("标题", "内容", 1L, null);
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                forumPostService.updatePost(999L, owner, request));
    }
}
