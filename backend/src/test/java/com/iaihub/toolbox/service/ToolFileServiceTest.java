package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.FileListResponse;
import com.iaihub.toolbox.dto.FileUploadResponse;
import com.iaihub.toolbox.dto.ToolFileDTO;
import com.iaihub.toolbox.exception.FileValidationException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.model.User;
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

    private User testUser;

    @BeforeEach
    void setUp() {
        UploadConfig uploadConfig = new UploadConfig();
        uploadConfig.setBaseDir(tempDir.toString());
        // 默认配置：allowedExtensions 为空，跳过扩展名校验
        uploadConfig.setAllowedExtensions(List.of());

        testUser = User.builder().id(99L).username("tester").build();
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

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(1L);
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(file), null, 99L);

        // Then
        assertNotNull(response);
        assertEquals(toolId, response.getToolId());
        assertEquals(1, response.getFiles().size());
        assertEquals("test.py", response.getFiles().get(0).getOriginalName());
        verify(toolFileRepository).save(any(ToolFile.class));
    }

    // ========================================
    // T3.2 / T3.3: 任意扩展名放行（allowed-extensions 为空）
    // ========================================

    @Test
    void uploadFiles_acceptsAnyExtensionWhenWhitelistIsEmpty() {
        // Given: 模型权重、Notebook、Office、无扩展名 等
        Long toolId = 1L;
        MockMultipartFile safetensors = new MockMultipartFile(
                "files", "weights.safetensors", "application/octet-stream", "fake-weights".getBytes()
        );
        MockMultipartFile ipynb = new MockMultipartFile(
                "files", "analysis.ipynb", "application/x-ipynb+json", "{\"cells\":[]}".getBytes()
        );
        MockMultipartFile bin = new MockMultipartFile(
                "files", "model.v2.final.bin", "application/octet-stream", "bin".getBytes()
        );
        MockMultipartFile noExt = new MockMultipartFile(
                "files", "Dockerfile", "text/plain", "FROM alpine".getBytes()
        );

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.findByToolIdAndOriginalNameAndStatus(eq(toolId), anyString(), any()))
                .thenReturn(Optional.empty());
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(System.currentTimeMillis());
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(
                toolId, List.of(safetensors, ipynb, bin, noExt), null, 99L);

        // Then: 全部接受
        assertNotNull(response);
        assertEquals(4, response.getFiles().size());
        assertEquals("weights.safetensors", response.getFiles().get(0).getOriginalName());
        assertEquals("analysis.ipynb", response.getFiles().get(1).getOriginalName());
        assertEquals("model.v2.final.bin", response.getFiles().get(2).getOriginalName());
        assertEquals("Dockerfile", response.getFiles().get(3).getOriginalName());
        verify(toolFileRepository, times(4)).save(any(ToolFile.class));
    }

    @Test
    void uploadFiles_skipsExtensionCheckWhenAllowedExtensionsIsNull() {
        // Given: null 也视为空，跳过白名单
        UploadConfig cfg = new UploadConfig();
        cfg.setBaseDir(tempDir.toString());
        cfg.setAllowedExtensions(null);
        ToolFileService svc = new ToolFileService(toolFileRepository, toolRepository, cfg);

        Long toolId = 2L;
        MockMultipartFile file = new MockMultipartFile(
                "files", "evil.exe", "application/octet-stream", "x".getBytes()
        );

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.findByToolIdAndOriginalNameAndStatus(eq(toolId), anyString(), any()))
                .thenReturn(Optional.empty());
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(1L);
            return tf;
        });

        // When & Then: 不抛异常
        FileUploadResponse response = svc.uploadFiles(toolId, List.of(file), null, 99L);
        assertEquals(1, response.getFiles().size());
        assertEquals("evil.exe", response.getFiles().get(0).getOriginalName());
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

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(1L);
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(file), readmeContent, 99L);

        // Then
        assertTrue(response.isReadmeSaved());
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

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));

        // When & Then
        assertThrows(FileValidationException.class, () ->
                toolFileService.uploadFiles(toolId, List.of(file), null, 99L)
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
                toolFileService.uploadFiles(toolId, List.of(file), null, 99L)
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
        toolFileService.deleteToolFile(toolId, fileId, 99L);

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
                toolFileService.deleteToolFile(toolId, fileId, 99L)
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

    // ========================================
    // T023: 同名文件替换测试
    // ========================================

    @Test
    void uploadFiles_shouldReplaceExistingFileWithSameName() {
        // Given
        Long toolId = 1L;
        MockMultipartFile newFile = new MockMultipartFile(
                "files",
                "test.py",
                "text/x-python",
                "print('updated')".getBytes()
        );

        ToolFile existingFile = ToolFile.builder()
                .id(1L)
                .toolId(toolId)
                .originalName("test.py")
                .storedPath("1/test.py")
                .fileSize(100L)
                .contentType("text/x-python")
                .status(ToolFile.Status.NORMAL)
                .build();

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.findByToolIdAndOriginalNameAndStatus(
                toolId, "test.py", ToolFile.Status.NORMAL)).thenReturn(Optional.of(existingFile));
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            if (tf.getId() == null) {
                tf.setId(2L); // new file gets new id
            }
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(newFile), null, 99L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals("test.py", response.getFiles().get(0).getOriginalName());
        // Verify old file was soft-deleted
        assertEquals(ToolFile.Status.DELETED, existingFile.getStatus());
        verify(toolFileRepository, times(2)).save(any(ToolFile.class)); // one for soft-delete, one for new
    }

    @Test
    void uploadFiles_shouldNotReplaceWhenNoExistingFileWithSameName() {
        // Given
        Long toolId = 1L;
        MockMultipartFile newFile = new MockMultipartFile(
                "files",
                "unique.py",
                "text/x-python",
                "print('new')".getBytes()
        );

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));
        when(toolFileRepository.findByToolIdAndOriginalNameAndStatus(
                toolId, "unique.py", ToolFile.Status.NORMAL)).thenReturn(Optional.empty());
        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            tf.setId(3L);
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(newFile), null, 99L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals("unique.py", response.getFiles().get(0).getOriginalName());
        verify(toolFileRepository, times(1)).save(any(ToolFile.class)); // only new file saved
    }

    @Test
    void uploadFiles_shouldHandleMultipleFilesWithMixedReplacements() {
        // Given
        Long toolId = 1L;
        MockMultipartFile newFile1 = new MockMultipartFile(
                "files", "existing.py", "text/x-python", "updated content".getBytes()
        );
        MockMultipartFile newFile2 = new MockMultipartFile(
                "files", "new.py", "text/x-python", "new file content".getBytes()
        );

        ToolFile existingFile = ToolFile.builder()
                .id(10L)
                .toolId(toolId)
                .originalName("existing.py")
                .storedPath("1/existing.py")
                .fileSize(50L)
                .contentType("text/x-python")
                .status(ToolFile.Status.NORMAL)
                .build();

        Tool tool = Tool.builder().uploader(testUser).id(toolId).name("TestTool").build();
        when(toolRepository.findByIdAndStatusNormal(toolId)).thenReturn(Optional.of(tool));

        // First file exists, second doesn't
        when(toolFileRepository.findByToolIdAndOriginalNameAndStatus(
                toolId, "existing.py", ToolFile.Status.NORMAL)).thenReturn(Optional.of(existingFile));
        when(toolFileRepository.findByToolIdAndOriginalNameAndStatus(
                toolId, "new.py", ToolFile.Status.NORMAL)).thenReturn(Optional.empty());

        when(toolFileRepository.save(any(ToolFile.class))).thenAnswer(invocation -> {
            ToolFile tf = invocation.getArgument(0);
            if (tf.getId() == null) {
                tf.setId(System.currentTimeMillis());
            }
            return tf;
        });

        // When
        FileUploadResponse response = toolFileService.uploadFiles(toolId, List.of(newFile1, newFile2), null, 99L);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getFiles().size());
        // Verify old existing.py was soft-deleted
        assertEquals(ToolFile.Status.DELETED, existingFile.getStatus());
        // save called: 1 for soft-delete existing + 2 for new files = 3
        verify(toolFileRepository, times(3)).save(any(ToolFile.class));
    }
}
