package com.iaihub.toolbox.service.feedback;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.feedback.FeedbackCreateRequest;
import com.iaihub.toolbox.dto.feedback.FeedbackDTO;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.feedback.FeedbackCategory;
import com.iaihub.toolbox.model.feedback.FeedbackMessage;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.feedback.FeedbackMessageRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackMessageRepository feedbackMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletRequest httpRequest;

    private FeedbackService feedbackService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackMessageRepository, userRepository);

        testUser = User.builder()
            .id(1L).username("wangbao").nickname("王宝")
            .role(Role.USER).build();

        adminUser = User.builder()
            .id(2L).username("admin").nickname("管理员")
            .role(Role.ADMIN).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===== submit tests =====

    @Test
    void submit_anonymous_userIdNull_ipHashSet() {
        // No authentication in SecurityContext
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);

        FeedbackCreateRequest request = new FeedbackCreateRequest(
            "建议增加暗色模式", "路人", null, null);

        FeedbackMessage saved = FeedbackMessage.builder()
            .id(1L)
            .content("建议增加暗色模式")
            .nickname("路人")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .ipHash("some-hash")
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.save(any(FeedbackMessage.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submit(request, httpRequest);

        assertNotNull(result);
        assertEquals("建议增加暗色模式", result.content());
        assertEquals("路人", result.nickname());
        assertEquals("SUGGESTION", result.category());
        assertNull(result.adminReply());

        verify(feedbackMessageRepository).save(argThat(msg -> {
            assertNull(msg.getUser());
            assertNotNull(msg.getIpHash());
            return true;
        }));
    }

    @Test
    void submit_loggedIn_userIdAssociated() {
        // Set authentication
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(testUser, null, java.util.Collections.emptyList()));

        FeedbackCreateRequest request = new FeedbackCreateRequest(
            "文件上传偶尔失败", null, null, null);

        FeedbackMessage saved = FeedbackMessage.builder()
            .id(2L)
            .content("文件上传偶尔失败")
            .nickname("王宝")
            .user(testUser)
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.save(any(FeedbackMessage.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submit(request, httpRequest);

        assertNotNull(result);
        assertEquals("王宝", result.nickname());

        verify(feedbackMessageRepository).save(argThat(msg -> {
            assertEquals(testUser, msg.getUser());
            assertNull(msg.getIpHash());
            assertEquals("王宝", msg.getNickname());
            return true;
        }));
    }

    @Test
    void submit_xssContent_sanitized() {
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);

        FeedbackCreateRequest request = new FeedbackCreateRequest(
            "<script>alert(1)</script>", "test", null, null);

        FeedbackMessage saved = FeedbackMessage.builder()
            .id(3L)
            .content("alert(1)")
            .nickname("test")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.save(any(FeedbackMessage.class))).thenReturn(saved);

        feedbackService.submit(request, httpRequest);

        verify(feedbackMessageRepository).save(argThat(msg -> {
            assertFalse(msg.getContent().contains("<script>"));
            return true;
        }));
    }

    @Test
    void submit_validCategory_accepted() {
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);

        FeedbackCreateRequest request = new FeedbackCreateRequest(
            "发现一个bug", null, null, "BUG_REPORT");

        FeedbackMessage saved = FeedbackMessage.builder()
            .id(4L)
            .content("发现一个bug")
            .category(FeedbackCategory.BUG_REPORT)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.save(any(FeedbackMessage.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submit(request, httpRequest);

        assertEquals("BUG_REPORT", result.category());
    }

    @Test
    void submit_noCategory_defaultsToSuggestion() {
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);

        FeedbackCreateRequest request = new FeedbackCreateRequest(
            "一些建议", null, null, null);

        FeedbackMessage saved = FeedbackMessage.builder()
            .id(5L)
            .content("一些建议")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.save(any(FeedbackMessage.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submit(request, httpRequest);

        assertEquals("SUGGESTION", result.category());
    }

    @Test
    void submit_invalidCategory_defaultsToSuggestion() {
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);

        FeedbackCreateRequest request = new FeedbackCreateRequest(
            "一些建议", null, null, "INVALID_CATEGORY");

        FeedbackMessage saved = FeedbackMessage.builder()
            .id(6L)
            .content("一些建议")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.save(any(FeedbackMessage.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submit(request, httpRequest);

        assertEquals("SUGGESTION", result.category());
    }

    // ===== list tests =====

    @Test
    void list_defaultPagination() {
        FeedbackMessage msg = FeedbackMessage.builder()
            .id(1L).content("测试").nickname("用户")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        Page<FeedbackMessage> page = new PageImpl<>(List.of(msg));
        when(feedbackMessageRepository.findByStatusOrderByCreatedAtDesc(
            eq(FeedbackMessage.Status.NORMAL), any(PageRequest.class)))
            .thenReturn(page);

        PageResponse<FeedbackDTO> result = feedbackService.list(null, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void list_categoryFilter() {
        FeedbackMessage msg = FeedbackMessage.builder()
            .id(1L).content("bug反馈").nickname("用户")
            .category(FeedbackCategory.BUG_REPORT)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        Page<FeedbackMessage> page = new PageImpl<>(List.of(msg));
        when(feedbackMessageRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
            eq(FeedbackCategory.BUG_REPORT), eq(FeedbackMessage.Status.NORMAL), any(PageRequest.class)))
            .thenReturn(page);

        PageResponse<FeedbackDTO> result = feedbackService.list("BUG_REPORT", 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals("BUG_REPORT", result.getContent().get(0).category());
    }

    @Test
    void list_softDeletedFiltered() {
        // Only NORMAL status messages are queried
        Page<FeedbackMessage> page = new PageImpl<>(List.of());
        when(feedbackMessageRepository.findByStatusOrderByCreatedAtDesc(
            eq(FeedbackMessage.Status.NORMAL), any(PageRequest.class)))
            .thenReturn(page);

        PageResponse<FeedbackDTO> result = feedbackService.list(null, 0, 20);

        assertEquals(0, result.getContent().size());
        // Verify only NORMAL status is queried (DELETED are excluded by design)
        verify(feedbackMessageRepository).findByStatusOrderByCreatedAtDesc(
            eq(FeedbackMessage.Status.NORMAL), any(PageRequest.class));
    }

    // ===== reply tests =====

    @Test
    void reply_success() {
        FeedbackMessage message = FeedbackMessage.builder()
            .id(1L).content("建议").nickname("用户")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.findByIdAndStatusNormal(1L))
            .thenReturn(java.util.Optional.of(message));
        when(feedbackMessageRepository.save(any(FeedbackMessage.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        FeedbackDTO result = feedbackService.reply(1L, "感谢建议，已排期", adminUser);

        assertNotNull(result.adminReply());
        verify(feedbackMessageRepository).save(argThat(msg -> {
            assertEquals("感谢建议，已排期", msg.getAdminReply());
            assertEquals(adminUser, msg.getRepliedBy());
            assertNotNull(msg.getRepliedAt());
            return true;
        }));
    }

    @Test
    void reply_notFound_throws404() {
        when(feedbackMessageRepository.findByIdAndStatusNormal(99999L))
            .thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> feedbackService.reply(99999L, "回复", adminUser));
    }

    // ===== delete tests =====

    @Test
    void delete_success() {
        FeedbackMessage message = FeedbackMessage.builder()
            .id(1L).content("建议").nickname("用户")
            .category(FeedbackCategory.SUGGESTION)
            .status(FeedbackMessage.Status.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();

        when(feedbackMessageRepository.findByIdAndStatusNormal(1L))
            .thenReturn(java.util.Optional.of(message));
        when(feedbackMessageRepository.save(any(FeedbackMessage.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        feedbackService.delete(1L);

        verify(feedbackMessageRepository).save(argThat(msg ->
            msg.getStatus() == FeedbackMessage.Status.DELETED));
    }

    @Test
    void delete_notFound_throws404() {
        when(feedbackMessageRepository.findByIdAndStatusNormal(99999L))
            .thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> feedbackService.delete(99999L));
    }
}
