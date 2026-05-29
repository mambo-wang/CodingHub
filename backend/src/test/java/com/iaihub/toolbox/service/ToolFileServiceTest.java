package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.FileListResponse;
import com.iaihub.toolbox.dto.FileUploadResponse;
import com.iaihub.toolbox.dto.ToolFileDTO;
import com.iaihub.toolbox.exception.FileValidationException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolFileServiceTest {

    @Mock
    private ToolFileRepository toolFileRepository;

    @Mock
    private ToolRepository toolRepository;

    @TempDir
    Path tempDir;

    private ToolFileService toolFileService;

    @BeforeEach
    void setUp() {
        UploadConfig uploadConfig = new UploadConfig();
        uploadConfig.setBaseDir(tempDir.toString());
        uploadConfig.setAllowedExtensions(List.of("zip", "py", "js", "md", "txt", "json"));

        toolFileService = new ToolFileService(
                toolFileRepository,
                toolRepository,
                uploadConfig
        );
    }

    @Test
    void uploadFiles_createsToolFolderAndSavesFiles() {
        // Given
        Long toolId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.py",
                "text/x-python",
                "print('hello')".getBytes()
        );

        Tool tool = Tool.builder().id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(1L);
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(file), null);

        // Then
        assertNotNull(response);
        assertEquals(toolId, response.getToolId());
        assertEquals(1, response.getFiles().size());
        assertEquals("test.py", response.getFiles().get(0).getOriginalName());
        verify(toolFileRepository).save(any(ToolFile.class));
    }

    @Test
    void uploadFiles_savesReadmeContent() {
        // Given
        Long toolId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.py",
                "text/x-python",
                "code".getBytes()
        );
        String readmeContent = "# Test Tool\n\nThis is a test.";

        Tool tool = Tool.builder().id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(1L);
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(file), readmeContent);

        // Then
        assertTrue(response.isReadmeSaved());
    }

    @Test
    void uploadFiles_throwsExceptionForInvalidFileType() {
        // Given
        Long toolId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.exe",
                "application/octet-stream",
                "malicious".getBytes()
        );

        Tool tool = Tool.builder().id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));

        // When & Then
        assertThrows(FileValidationException.class, () ->
                toolFileService.uploadFiles(toolId, List.of(file), null)
        );
    }

    @Test
    void uploadFiles_throwsExceptionForFileTooLarge() {
        // Given
        Long toolId = 1L;
        byte[] largeContent = new byte[(int) (51 * 1024 * 1024)]; // 51MB > 50MB limit
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "large.py",
                "text/x-python",
                largeContent
        );

        Tool tool = Tool.builder().id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));

        // When & Then
        assertThrows(FileValidationException.class, () ->
                toolFileService.uploadFiles(toolId, List.of(file), null)
        );
    }

    @Test
    void uploadFiles_throwsExceptionForToolNotFound() {
        // Given
        Long toolId = 999L;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.py",
                "text/x-python",
                "code".getBytes()
        );

        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                toolFileService.uploadFiles(toolId, List.of(file), null)
        );
    }

    @Test
    void getToolFiles_returnsFileList() {
        // Given
        Long toolId = 1L;
        ToolFile file = ToolFile.builder()
                .id(1L)
                .toolId(toolId)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(1024L)
                .contentType("text/x-python")
                .build();

        when(toolFileRepository.findByToolIdAndStatusNormal(toolId)).thenReturn(List.of(file));

        // When
        FileListResponse response = toolFileService.getToolFiles(toolId);

        // Then
        assertNotNull(response);
        assertEquals(toolId, response.getToolId());
        assertEquals(1, response.getFiles().size());
    }

    @Test
    void deleteToolFile_deletesFileAndRecord() {
        // Given
        Long toolId = 1L;
        Long fileId = 1L;
        ToolFile file = ToolFile.builder()
                .id(fileId)
                .toolId(toolId)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(1024L)
                .status(ToolFile.Status.NORMAL)
                .build();

        when(toolFileRepository.findByIdAndToolId(fileId, toolId)).thenReturn(Optional.of(file));

        // When
        toolFileService.deleteToolFile(toolId, fileId);

        // Then
        verify(toolFileRepository).deleteById(fileId);
    }

    @Test
    void deleteToolFile_throwsExceptionWhenNotFound() {
        // Given
        Long toolId = 1L;
        Long fileId = 999L;

        when(toolFileRepository.findByIdAndToolId(fileId, toolId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                toolFileService.deleteToolFile(toolId, fileId)
        );
    }

    @Test
    void cleanupToolFiles_deletesAllFilesForTool() {
        // Given
        Long toolId = 1L;

        // When
        toolFileService.cleanupToolFiles(toolId);

        // Then
        verify(toolFileRepository).deleteByToolId(toolId);
    }
}
