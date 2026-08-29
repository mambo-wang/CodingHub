package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.Plugin;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PluginCounterBackfillRunnerTest {

    @Mock
    private PluginRepository pluginRepository;
    @Mock
    private UnifiedFavoriteRepository favoriteRepository;

    private PluginCounterBackfillRunner runner;

    @BeforeEach
    void setUp() {
        runner = new PluginCounterBackfillRunner(pluginRepository, favoriteRepository);
    }

    private Plugin plugin(Long id, int favoriteCount) {
        return Plugin.builder()
                .id(id).name("p" + id).version("1.0.0")
                .status(Plugin.Status.NORMAL)
                .viewCount(0).likeCount(0).commentCount(0).favoriteCount(favoriteCount)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void backfill_syncsFavoriteCountAndScore() {
        Plugin p = plugin(1L, 0);
        p.setScore(BigDecimal.ZERO);
        when(pluginRepository.findByStatus(Plugin.Status.NORMAL)).thenReturn(List.of(p));
        when(favoriteRepository.countByTargetTypeGroupByTargetId("PLUGIN"))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{1L, 3L}));

        runner.run();

        assertEquals(3, p.getFavoriteCount());
        assertEquals(0, new BigDecimal("12").compareTo(p.getScore())); // 3×4
        verify(pluginRepository).save(p);
    }

    @Test
    void backfill_normalizesNullPinned() {
        Plugin p = plugin(2L, 0);
        p.setPinned(null);
        when(pluginRepository.findByStatus(Plugin.Status.NORMAL)).thenReturn(List.of(p));
        when(favoriteRepository.countByTargetTypeGroupByTargetId("PLUGIN"))
                .thenReturn(List.of());

        runner.run();

        assertFalse(p.getPinned());
        verify(pluginRepository).save(p);
    }

    @Test
    void backfill_isIdempotent_noWriteWhenConsistent() {
        Plugin p = plugin(3L, 2);
        p.setScore(new BigDecimal("8")); // 2×4
        p.setPinned(false);
        when(pluginRepository.findByStatus(Plugin.Status.NORMAL)).thenReturn(List.of(p));
        when(favoriteRepository.countByTargetTypeGroupByTargetId("PLUGIN"))
                .thenReturn(java.util.List.<Object[]>of(new Object[]{3L, 2L}));

        runner.run();

        verify(pluginRepository, never()).save(any(Plugin.class));
    }
}
