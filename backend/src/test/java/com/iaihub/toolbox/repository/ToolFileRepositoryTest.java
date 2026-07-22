package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ToolFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ToolFileRepositoryTest {

    @Autowired
    private ToolFileRepository toolFileRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_persistsToolFile() {
        // Given
        ToolFile toolFile = ToolFile.builder()
                .toolId(1L)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(1024L)
                .contentType("text/x-python")
                .build();

        // When
        ToolFile saved = toolFileRepository.save(toolFile);

        // Then
        assertNotNull(saved.getId());
        assertEquals("test.py", saved.getOriginalName());
    }

    @Test
    void findByToolId_returnsFilesForTool() {
        // Given
        ToolFile file1 = toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("file1.py")
                .storedPath("tools/1/file1.py")
                .fileSize(100L)
                .build());
        ToolFile file2 = toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("file2.py")
                .storedPath("tools/1/file2.py")
                .fileSize(200L)
                .build());
        toolFileRepository.save(ToolFile.builder()
                .toolId(2L)
                .originalName("other.py")
                .storedPath("tools/2/other.py")
                .fileSize(300L)
                .build());

        // When
        List<ToolFile> files = toolFileRepository.findByToolId(1L);

        // Then
        assertEquals(2, files.size());
    }

    @Test
    void findByToolIdAndStatusNormal_returnsOnlyNormalFiles() {
        // Given
        toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("normal.py")
                .storedPath("tools/1/normal.py")
                .fileSize(100L)
                .status(ToolFile.Status.NORMAL)
                .build());
        toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("deleted.py")
                .storedPath("tools/1/deleted.py")
                .fileSize(100L)
                .status(ToolFile.Status.DELETED)
                .build());

        // When
        List<ToolFile> files = toolFileRepository.findByToolIdAndStatusNormal(1L);

        // Then
        assertEquals(1, files.size());
        assertEquals("normal.py", files.get(0).getOriginalName());
    }

    @Test
    void findByIdAndToolId_returnsFile() {
        // Given
        ToolFile saved = toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(100L)
                .build());

        // When
        Optional<ToolFile> found = toolFileRepository.findByIdAndToolId(saved.getId(), 1L);

        // Then
        assertTrue(found.isPresent());
        assertEquals("test.py", found.get().getOriginalName());
    }

    @Test
    void deleteByToolId_deletesAllFilesForTool() {
        // Given
        toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("file1.py")
                .storedPath("tools/1/file1.py")
                .fileSize(100L)
                .build());
        toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("file2.py")
                .storedPath("tools/1/file2.py")
                .fileSize(100L)
                .build());

        // When
        toolFileRepository.deleteByToolId(1L);

        // Then
        assertTrue(toolFileRepository.findByToolId(1L).isEmpty());
    }

    @Test
    void incrementDownloadCount_incrementsByOne() {
        // Given
        ToolFile saved = toolFileRepository.save(ToolFile.builder()
                .toolId(1L)
                .originalName("test.py")
                .storedPath("tools/1/test.py")
                .fileSize(100L)
                .build());
        assertEquals(0, saved.getDownloadCount());

        // When
        toolFileRepository.incrementDownloadCount(saved.getId());
        entityManager.clear();

        // Then
        ToolFile reloaded = toolFileRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, reloaded.getDownloadCount());
    }

    @Test
    void sumDownloadCountGroupByToolId_aggregatesPerTool() {
        // Given
        toolFileRepository.save(ToolFile.builder()
                .toolId(1L).originalName("a.py").storedPath("tools/1/a.py")
                .fileSize(100L).downloadCount(3).build());
        toolFileRepository.save(ToolFile.builder()
                .toolId(1L).originalName("b.py").storedPath("tools/1/b.py")
                .fileSize(100L).downloadCount(4).build());
        toolFileRepository.save(ToolFile.builder()
                .toolId(2L).originalName("c.py").storedPath("tools/2/c.py")
                .fileSize(100L).downloadCount(5).build());

        // When
        List<Object[]> rows = toolFileRepository.sumDownloadCountGroupByToolId(List.of(1L, 2L));

        // Then
        java.util.Map<Long, Long> sums = new java.util.HashMap<>();
        for (Object[] row : rows) {
            sums.put((Long) row[0], (Long) row[1]);
        }
        assertEquals(7L, sums.get(1L));
        assertEquals(5L, sums.get(2L));
    }
}
