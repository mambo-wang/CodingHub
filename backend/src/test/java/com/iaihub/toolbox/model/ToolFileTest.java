package com.iaihub.toolbox.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ToolFileTest {

    @Test
    void builder_createsToolFileWithAllFields() {
        // Given
        Long toolId = 1L;
        String originalName = "test.py";
        String storedPath = "tools/1/test.py";
        Long fileSize = 1024L;
        String contentType = "text/x-python";

        // When
        ToolFile toolFile = ToolFile.builder()
                .toolId(toolId)
                .originalName(originalName)
                .storedPath(storedPath)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();

        // Then
        assertEquals(toolId, toolFile.getToolId());
        assertEquals(originalName, toolFile.getOriginalName());
        assertEquals(storedPath, toolFile.getStoredPath());
        assertEquals(fileSize, toolFile.getFileSize());
        assertEquals(contentType, toolFile.getContentType());
    }

    @Test
    void defaultStatus_isNormal() {
        // When
        ToolFile toolFile = ToolFile.builder()
                .toolId(1L)
                .originalName("test.txt")
                .storedPath("tools/1/test.txt")
                .fileSize(100L)
                .build();

        // Then
        assertEquals(ToolFile.Status.NORMAL, toolFile.getStatus());
    }

    @Test
    void defaultDownloadCount_isZero() {
        // When
        ToolFile toolFile = ToolFile.builder()
                .toolId(1L)
                .originalName("test.txt")
                .storedPath("tools/1/test.txt")
                .fileSize(100L)
                .build();

        // Then
        assertEquals(0, toolFile.getDownloadCount());
    }

    @Test
    void onCreate_setsCreatedAt() {
        // Given
        ToolFile toolFile = ToolFile.builder()
                .toolId(1L)
                .originalName("test.txt")
                .storedPath("tools/1/test.txt")
                .fileSize(100L)
                .build();

        // When
        toolFile.onCreate();

        // Then
        assertNotNull(toolFile.getCreatedAt());
        assertEquals(LocalDateTime.now().getYear(), toolFile.getCreatedAt().getYear());
    }

    @Test
    void fileSize_cannotBeNegative() {
        // Given
        ToolFile toolFile = ToolFile.builder()
                .toolId(1L)
                .originalName("test.txt")
                .storedPath("tools/1/test.txt")
                .fileSize(0L)
                .build();

        // Then
        assertEquals(0L, toolFile.getFileSize());
    }
}
