package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.FileListResponse;
import com.iaihub.toolbox.dto.FileUploadResponse;
import com.iaihub.toolbox.dto.ToolFileDTO;
import com.iaihub.toolbox.exception.FileValidationException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.service.ToolFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolFileService toolFileService;

    @Test
    @WithMockUser
    void uploadFiles_returnsSuccessResponse() throws Exception {
        // Given
        Long toolId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.py",
                "text/x-python",
                "print('hello')".getBytes()
        );
        MockMultipartFile readme = new MockMultipartFile(
                "readme",
                "",
                "text/plain",
                "# Test".getBytes()
        );

        ToolFileDTO fileDto = ToolFileDTO.builder()
                .id(1L)
                .toolId(toolId)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(1024L)
                .build();

        FileUploadResponse response = FileUploadResponse.builder()
                .toolId(toolId)
                .files(List.of(fileDto))
                .readmeSaved(true)
                .build();

        when(toolFileService.uploadFiles(eq(toolId), anyList(), anyString(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(multipart("/api/v1/tools/{toolId}/files", toolId)
                        .file(file)
                        .file(readme)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.toolId").value(toolId))
                .andExpect(jsonPath("$.data.files[0].originalName").value("test.py"))
                .andExpect(jsonPath("$.data.readmeSaved").value(true));
    }

    @Test
    @WithMockUser
    void uploadFiles_returnsBadRequestForInvalidFileType() throws Exception {
        // Given
        Long toolId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.exe",
                "application/octet-stream",
                "malicious".getBytes()
        );

        when(toolFileService.uploadFiles(eq(toolId), anyList(), isNull(), any()))
                .thenThrow(new FileValidationException("不支持的文件类型: .exe"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/tools/{toolId}/files", toolId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser
    void getToolFiles_returnsFileList() throws Exception {
        // Given
        Long toolId = 1L;
        ToolFileDTO fileDto = ToolFileDTO.builder()
                .id(1L)
                .toolId(toolId)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(1024L)
                .build();

        FileListResponse response = FileListResponse.builder()
                .toolId(toolId)
                .folderPath("tools/1")
                .files(List.of(fileDto))
                .readmeExists(true)
                .build();

        when(toolFileService.getToolFiles(toolId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/tools/{toolId}/files", toolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.toolId").value(toolId))
                .andExpect(jsonPath("$.data.files[0].originalName").value("test.py"))
                .andExpect(jsonPath("$.data.readmeExists").value(true));
    }

    @Test
    @WithMockUser
    void deleteToolFile_returnsSuccess() throws Exception {
        // Given
        Long toolId = 1L;
        Long fileId = 1L;

        doNothing().when(toolFileService).deleteToolFile(toolId, fileId, any());

        // When & Then
        mockMvc.perform(delete("/api/v1/tools/{toolId}/files/{fileId}", toolId, fileId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    void deleteToolFile_returnsNotFoundWhenFileNotExists() throws Exception {
        // Given
        Long toolId = 1L;
        Long fileId = 999L;

        doThrow(new ResourceNotFoundException("文件不存在"))
                .when(toolFileService).deleteToolFile(toolId, fileId, any());

        // When & Then
        mockMvc.perform(delete("/api/v1/tools/{toolId}/files/{fileId}", toolId, fileId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
