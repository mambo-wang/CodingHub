package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.UnifiedFavorite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UnifiedFavoriteCountRepositoryTest {

    @Autowired
    private UnifiedFavoriteRepository unifiedFavoriteRepository;

    private UnifiedFavorite fav(Long userId, String targetType, Long targetId) {
        return UnifiedFavorite.builder()
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }

    @Test
    void countByTargetTypeAndTargetId_countsMatchingRows() {
        // Given
        unifiedFavoriteRepository.save(fav(1L, "TOOL", 100L));
        unifiedFavoriteRepository.save(fav(2L, "TOOL", 100L));
        unifiedFavoriteRepository.save(fav(3L, "TOOL", 100L));
        unifiedFavoriteRepository.save(fav(1L, "TOOL", 200L));
        unifiedFavoriteRepository.save(fav(1L, "FORUM_POST", 100L));

        // When / Then
        assertEquals(3L, unifiedFavoriteRepository.countByTargetTypeAndTargetId("TOOL", 100L));
        assertEquals(1L, unifiedFavoriteRepository.countByTargetTypeAndTargetId("TOOL", 200L));
        assertEquals(0L, unifiedFavoriteRepository.countByTargetTypeAndTargetId("TOOL", 999L));
    }

    @Test
    void countByTargetTypeAndTargetIdIn_groupsByTargetId() {
        // Given
        unifiedFavoriteRepository.save(fav(1L, "TOOL", 100L));
        unifiedFavoriteRepository.save(fav(2L, "TOOL", 100L));
        unifiedFavoriteRepository.save(fav(1L, "TOOL", 200L));
        unifiedFavoriteRepository.save(fav(1L, "FORUM_POST", 100L));

        // When
        List<Object[]> rows = unifiedFavoriteRepository.countByTargetTypeAndTargetIdIn("TOOL", List.of(100L, 200L, 300L));

        // Then
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        assertEquals(2L, counts.get(100L));
        assertEquals(1L, counts.get(200L));
        assertNull(counts.get(300L));
    }
}
