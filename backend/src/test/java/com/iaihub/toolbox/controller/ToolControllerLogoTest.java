package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.GlobalExceptionHandler;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.mcp.McpNotificationService;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 6.3: ToolController Logo 绑定端点单元测试（MockMvc standalone）
 * 覆盖：所有者设置成功、非所有者非管理员 403、工具不存在 404
 */
@ExtendWith(MockitoExtension.class)
class ToolControllerLogoTest {

    @Mock
    private ToolService toolService;

    @Mock
    private McpNotificationService mcpNotificationService;

    private MockMvc mockMvc;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("owner").role(Role.USER).build();

        ToolController controller = new ToolController(toolService, mcpNotificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return currentUser;
                    }
                })
                .build();
    }

    @Test
    void updateLogo_ownerSuccess_returns200() throws Exception {
        doNothing().when(toolService).updateLogo(eq(10L), eq("/images/logo.png"), any(User.class));

        mockMvc.perform(post("/api/v1/tools/10/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\": \"/images/logo.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(toolService).updateLogo(eq(10L), eq("/images/logo.png"), any(User.class));
    }

    @Test
    void updateLogo_nonOwnerNonAdmin_returns403() throws Exception {
        doThrow(new ForbiddenException("无权操作此内容"))
                .when(toolService).updateLogo(eq(10L), anyString(), any(User.class));

        mockMvc.perform(post("/api/v1/tools/10/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\": \"/images/logo.png\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateLogo_toolNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("工具不存在或已删除"))
                .when(toolService).updateLogo(eq(99L), anyString(), any(User.class));

        mockMvc.perform(post("/api/v1/tools/99/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\": \"/images/logo.png\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
