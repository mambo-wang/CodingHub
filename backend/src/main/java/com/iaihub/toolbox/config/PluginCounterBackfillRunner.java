package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.Plugin;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 启动幂等回填：收藏数（favorite_count 为新增列，存量数据需按 favorite 表实际计数修正并联动热度分）
 * 与 pinned 归一（ddl-auto 加列后存量行为 NULL，会破坏 hot 排序的 pinned DESC）。
 * 幂等：数值一致时零写入，重复启动无副作用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class PluginCounterBackfillRunner implements CommandLineRunner {

    private final PluginRepository pluginRepository;
    private final UnifiedFavoriteRepository favoriteRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<Long, Long> actualCounts = favoriteRepository
                .countByTargetTypeGroupByTargetId("PLUGIN").stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        List<Plugin> plugins = pluginRepository.findByStatus(Plugin.Status.NORMAL);
        int fixed = 0;
        for (Plugin p : plugins) {
            int actual = actualCounts.getOrDefault(p.getId(), 0L).intValue();
            boolean dirty = false;
            if (!Objects.equals(p.getFavoriteCount(), actual)) {
                p.setFavoriteCount(actual);
                dirty = true;
            }
            if (p.getPinned() == null) {
                p.setPinned(false);
                dirty = true;
            }
            if (dirty) {
                p.updateScore();
                pluginRepository.save(p);
                fixed++;
            }
        }
        if (fixed > 0) {
            log.info("Plugin counter backfill: fixed {} plugin(s)", fixed);
        }
    }
}
