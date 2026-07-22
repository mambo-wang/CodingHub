package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolFileDownloadCountTest {

    @Mock
    private ToolFileRepository toolFileRepository;

    @Mock
    private ToolRepository toolRepository;

    private UploadConfig uploadConfig;

    private ToolFileService toolFileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        uploadConfig = new UploadConfig();
        uploadConfig.setBaseDir(tempDir.toString());
        toolFileService = new ToolFileService(toolFileRepository, toolRepository, uploadConfig);
    }

    @Test
    void downloadFile_incrementsDownloadCount() throws Exception {
        // Given：物理文件存在
        Path file = tempDir.resolve("1").resolve("test.py");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "print('hi')");

        ToolFile toolFile = ToolFile.builder()
                .id(10L)
                .toolId(1L)
                .originalName("test.py")
                .storedPath(Path.of("1", "test.py").toString())
                .fileSize(11L)
                .build();
        when(toolFileRepository.findByIdAndToolId(10L, 1L)).thenReturn(Optional.of(toolFile));

        // When
        ToolFile result = toolFileService.downloadFile(1L, 10L);

        // Then：触发自增
        assertNotNull(result);
        verify(toolFileRepository).incrementDownloadCount(10L);
    }

    @Test
    void downloadFile_fileNotFound_throwsAndDoesNotIncrement() {
        // Given：仓库无记录
        when(toolFileRepository.findByIdAndToolId(99L, 1L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> toolFileService.downloadFile(1L, 99L));
        verify(toolFileRepository, never()).incrementDownloadCount(anyLong());
    }

    @Test
    void downloadFile_physicalFileMissing_throwsAndDoesNotIncrement() {
        // Given：数据库有记录但物理文件不存在
        ToolFile toolFile = ToolFile.builder()
                .id(11L)
                .toolId(1L)
                .originalName("gone.py")
                .storedPath(Path.of("1", "gone.py").toString())
                .fileSize(1L)
                .build();
        when(toolFileRepository.findByIdAndToolId(11L, 1L)).thenReturn(Optional.of(toolFile));

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> toolFileService.downloadFile(1L, 11L));
        verify(toolFileRepository, never()).incrementDownloadCount(anyLong());
    }
}
