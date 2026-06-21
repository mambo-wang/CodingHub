package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.CreateToolRequest;
import com.iaihub.toolbox.dto.ToolDetailDTO;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.dto.UpdateToolRequest;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.ToolCommentRepository;
import com.iaihub.toolbox.repository.ToolLikeRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ToolCommentRepository toolCommentRepository;

    @Mock
    private ToolLikeRepository toolLikeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ToolFileService toolFileService;

    private ToolService toolService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        toolService = new ToolService(
                toolRepository,
                toolCommentRepository,
                toolLikeRepository,
                categoryRepository,
                userRepository,
                toolFileService
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test User")
                .password("password")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testCategory = Category.builder()
                .id(1L)
                .name("计算机视觉")
                .icon("👁️")
                .sortOrder(1)
                .build();
    }

    // ========================================
    // T022: 创建工具 - 版本号字段测试
    // ========================================

    @Test
    void createTool_shouldSaveWithVersion() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("图像识别工具")
                .categoryId(1L)
                .content("强大的AI工具")
                .version("1.0.0")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "图像识别工具", 1L, 1L, Tool.Status.NORMAL)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Tool savedTool = Tool.builder()
                .id(1L)
                .name("图像识别工具")
                .category(testCategory)
                .content("强大的AI工具")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        when(toolRepository.save(any(Tool.class))).thenReturn(savedTool);
        when(toolRepository.findByIdAndStatusNormalWithRelations(1L)).thenReturn(Optional.of(savedTool));

        // When
        ToolSummaryDTO result = toolService.createTool(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals("图像识别工具", result.getName());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("计算机视觉", result.getCategoryName());
        verify(toolRepository).save(any(Tool.class));
    }

    @Test
    void createTool_shouldSaveWithPreReleaseVersion() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("Beta工具")
                .categoryId(1L)
                .content("测试版")
                .version("2.0.0-beta1")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "Beta工具", 1L, 1L, Tool.Status.NORMAL)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Tool savedTool = Tool.builder()
                .id(2L)
                .name("Beta工具")
                .category(testCategory)
                .content("测试版")
                .version("2.0.0-beta1")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        when(toolRepository.save(any(Tool.class))).thenReturn(savedTool);
        when(toolRepository.findByIdAndStatusNormalWithRelations(2L)).thenReturn(Optional.of(savedTool));

        // When
        ToolSummaryDTO result = toolService.createTool(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals("2.0.0-beta1", result.getVersion());
    }

    // ========================================
    // T022: 版本号唯一性校验测试（同用户+同分类）
    // ========================================

    @Test
    void createTool_shouldAllowSameNameInDifferentCategory() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("通用工具")
                .categoryId(2L) // different category
                .content("描述")
                .version("1.0.0")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "通用工具", 1L, 2L, Tool.Status.NORMAL)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(
                Category.builder().id(2L).name("语音处理").icon("🎵").build()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Tool savedTool = Tool.builder()
                .id(3L)
                .name("通用工具")
                .category(Category.builder().id(2L).name("语音处理").icon("🎵").build())
                .content("描述")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        when(toolRepository.save(any(Tool.class))).thenReturn(savedTool);
        when(toolRepository.findByIdAndStatusNormalWithRelations(3L)).thenReturn(Optional.of(savedTool));

        // When
        ToolSummaryDTO result = toolService.createTool(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals("通用工具", result.getName());
    }

    @Test
    void createTool_shouldAllowSameNameForDifferentUser() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("通用工具")
                .categoryId(1L)
                .content("描述")
                .version("1.0.0")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "通用工具", 2L, 1L, Tool.Status.NORMAL)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                User.builder().id(2L).username("other").nickname("Other").password("pwd").build()));

        Tool savedTool = Tool.builder()
                .id(4L)
                .name("通用工具")
                .category(testCategory)
                .content("描述")
                .version("1.0.0")
                .uploader(User.builder().id(2L).username("other").nickname("Other").build())
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        when(toolRepository.save(any(Tool.class))).thenReturn(savedTool);
        when(toolRepository.findByIdAndStatusNormalWithRelations(4L)).thenReturn(Optional.of(savedTool));

        // When
        ToolSummaryDTO result = toolService.createTool(request, 2L);

        // Then
        assertNotNull(result);
        assertEquals("通用工具", result.getName());
    }

    @Test
    void createTool_shouldThrowDuplicateForSameUserSameCategorySameName() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("重复工具")
                .categoryId(1L)
                .content("描述")
                .version("1.0.0")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "重复工具", 1L, 1L, Tool.Status.NORMAL)).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, () ->
                toolService.createTool(request, 1L)
        );
    }

    @Test
    void createTool_shouldThrowWhenCategoryNotFound() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("测试工具")
                .categoryId(999L)
                .content("描述")
                .version("1.0.0")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "测试工具", 1L, 999L, Tool.Status.NORMAL)).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                toolService.createTool(request, 1L)
        );
    }

    @Test
    void createTool_shouldThrowWhenUserNotFound() {
        // Given
        CreateToolRequest request = CreateToolRequest.builder()
                .name("测试工具")
                .categoryId(1L)
                .content("描述")
                .version("1.0.0")
                .build();

        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatus(
                "测试工具", 999L, 1L, Tool.Status.NORMAL)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                toolService.createTool(request, 999L)
        );
    }

    // ========================================
    // T022: 更新工具 - 版本号字段测试
    // ========================================

    @Test
    void updateTool_shouldUpdateVersion() {
        // Given
        Tool existingTool = Tool.builder()
                .id(1L)
                .name("旧工具")
                .category(testCategory)
                .content("旧描述")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("旧工具")
                .categoryId(1L)
                .content("更新后的描述")
                .version("2.0.0")
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(existingTool));
        when(toolRepository.save(any(Tool.class))).thenReturn(existingTool);

        // When
        ToolDetailDTO result = toolService.updateTool(1L, request, testUser);

        // Then
        assertNotNull(result);
        assertEquals("2.0.0", result.getVersion());
        assertEquals("更新后的描述", result.getContent());
    }

    @Test
    void updateTool_shouldKeepOldVersionWhenVersionNotProvided() {
        // Given
        Tool existingTool = Tool.builder()
                .id(1L)
                .name("旧工具")
                .category(testCategory)
                .content("旧描述")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("旧工具")
                .categoryId(1L)
                .content("更新内容但不变版本")
                .version(null)  // version not provided
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(existingTool));
        when(toolRepository.save(any(Tool.class))).thenReturn(existingTool);

        // When
        ToolDetailDTO result = toolService.updateTool(1L, request, testUser);

        // Then
        assertNotNull(result);
        assertEquals("1.0.0", result.getVersion()); // version unchanged
    }

    @Test
    void updateTool_shouldKeepOldVersionWhenVersionBlank() {
        // Given
        Tool existingTool = Tool.builder()
                .id(1L)
                .name("旧工具")
                .category(testCategory)
                .content("旧描述")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("旧工具")
                .categoryId(1L)
                .content("更新内容")
                .version("")  // blank version
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(existingTool));
        when(toolRepository.save(any(Tool.class))).thenReturn(existingTool);

        // When
        ToolDetailDTO result = toolService.updateTool(1L, request, testUser);

        // Then
        assertEquals("1.0.0", result.getVersion());
    }

    // ========================================
    // T022: 更新工具 - 唯一性校验测试
    // ========================================

    @Test
    void updateTool_shouldThrowForbiddenWhenNotOwner() {
        // Given
        Tool existingTool = Tool.builder()
                .id(1L)
                .name("别人的工具")
                .category(testCategory)
                .content("内容")
                .version("1.0.0")
                .uploader(User.builder().id(2L).username("other").build())
                .status(Tool.Status.NORMAL)
                .build();

        User nonOwnerUser = User.builder().id(1L).username("testuser").role(Role.USER).build();

        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("别人的工具")
                .categoryId(1L)
                .content("尝试修改")
                .version("2.0.0")
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(existingTool));

        // When & Then
        assertThrows(ForbiddenException.class, () ->
                toolService.updateTool(1L, request, nonOwnerUser)
        );
    }

    @Test
    void updateTool_shouldThrowDuplicateWhenNameChangedToExistingInSameCategory() {
        // Given
        Tool existingTool = Tool.builder()
                .id(1L)
                .name("我的工具")
                .category(testCategory)
                .content("内容")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .build();

        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("已存在的工具")  // change to existing name
                .categoryId(1L)
                .content("更新内容")
                .version("2.0.0")
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(existingTool));
        when(toolRepository.existsByNameAndUploaderIdAndCategoryIdAndStatusAndIdNot(
                "已存在的工具", 1L, 1L, Tool.Status.NORMAL, 1L)).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, () ->
                toolService.updateTool(1L, request, testUser)
        );
    }

    @Test
    void updateTool_shouldThrowWhenToolNotFound() {
        // Given
        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("不存在的工具")
                .categoryId(1L)
                .content("内容")
                .version("1.0.0")
                .build();

        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                toolService.updateTool(999L, request, testUser)
        );
    }

    // ========================================
    // T022: 获取工具 - version 字段包含在响应中
    // ========================================

    @Test
    void getToolById_shouldReturnVersion() {
        // Given
        Tool tool = Tool.builder()
                .id(1L)
                .name("测试工具")
                .category(testCategory)
                .content("描述")
                .version("3.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .score(java.math.BigDecimal.ZERO)
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));

        // When
        ToolDetailDTO result = toolService.getToolById(1L);

        // Then
        assertNotNull(result);
        assertEquals("测试工具", result.getName());
        assertEquals("3.0.0", result.getVersion());
        assertEquals("计算机视觉", result.getCategoryName());
    }

    @Test
    void getToolById_shouldThrowWhenNotFound() {
        // Given
        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                toolService.getToolById(999L)
        );
    }

    // ========================================
    // T022: deleteTool - version 无关但确保覆盖
    // ========================================

    @Test
    void deleteTool_shouldSoftDeleteAndCleanupFiles() {
        // Given
        Tool tool = Tool.builder()
                .id(1L)
                .name("待删除工具")
                .category(testCategory)
                .content("内容")
                .version("1.0.0")
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));

        // When
        toolService.deleteTool(1L, testUser);

        // Then
        verify(toolFileService).cleanupToolFiles(1L);
        assertEquals(Tool.Status.DELETED, tool.getStatus());
        verify(toolRepository).save(tool);
    }

    @Test
    void deleteTool_shouldThrowForbiddenWhenNotOwner() {
        // Given
        Tool tool = Tool.builder()
                .id(1L)
                .name("别人的工具")
                .category(testCategory)
                .content("内容")
                .version("1.0.0")
                .uploader(User.builder().id(2L).username("other").build())
                .status(Tool.Status.NORMAL)
                .build();

        User nonOwnerUser = User.builder().id(1L).username("testuser").role(Role.USER).build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));

        // When & Then
        assertThrows(ForbiddenException.class, () ->
                toolService.deleteTool(1L, nonOwnerUser)
        );
    }

    @Test
    void deleteTool_shouldAllowAdminToDeleteOthersTool() {
        Tool tool = Tool.builder()
                .id(1L)
                .name("别人的工具")
                .category(testCategory)
                .content("内容")
                .version("1.0.0")
                .uploader(User.builder().id(2L).username("other").role(Role.USER).build())
                .status(Tool.Status.NORMAL)
                .build();

        User adminUser = User.builder()
                .id(99L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));

        // Should NOT throw - admin can delete anyone's tool
        assertDoesNotThrow(() -> toolService.deleteTool(1L, adminUser));
        assertEquals(Tool.Status.DELETED, tool.getStatus());
    }

    @Test
    void updateTool_shouldAllowAdminToUpdateOthersTool() {
        Tool existingTool = Tool.builder()
                .id(1L)
                .name("别人的工具")
                .category(testCategory)
                .content("旧内容")
                .version("1.0.0")
                .uploader(User.builder().id(2L).username("other").role(Role.USER).build())
                .status(Tool.Status.NORMAL)
                .build();

        User adminUser = User.builder()
                .id(99L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        UpdateToolRequest request = UpdateToolRequest.builder()
                .name("别人的工具")
                .categoryId(1L)
                .content("管理员更新的内容")
                .version("2.0.0")
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(existingTool));
        when(toolRepository.save(any(Tool.class))).thenReturn(existingTool);

        // Should NOT throw - admin can update anyone's tool
        ToolDetailDTO result = assertDoesNotThrow(() -> toolService.updateTool(1L, request, adminUser));
        assertNotNull(result);
    }

    @Test
    void deleteTool_shouldThrowForbiddenWhenNotOwnerAndNotAdmin() {
        Tool tool = Tool.builder()
                .id(1L)
                .name("别人的工具")
                .category(testCategory)
                .content("内容")
                .version("1.0.0")
                .uploader(User.builder().id(2L).username("other").role(Role.USER).build())
                .status(Tool.Status.NORMAL)
                .build();

        User regularUser = User.builder()
                .id(3L)
                .username("regular")
                .role(Role.USER)
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));

        assertThrows(ForbiddenException.class, () ->
                toolService.deleteTool(1L, regularUser)
        );
    }
}
