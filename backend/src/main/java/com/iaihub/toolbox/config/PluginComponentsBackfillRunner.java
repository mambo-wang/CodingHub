package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.Plugin;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.service.plugin.PluginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动幂等回填：按持久化 zip 原件重算所有正常插件的组件摘要（components）。
 *
 * <p>修复组件枚举逻辑（文件形式的 agents/commands/hooks 曾被 {@code listDirs} 漏掉）后，
 * 存量插件的 components 需要重新生成。幂等：内容一致时零写入，重复启动无副作用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(20)
public class PluginComponentsBackfillRunner implements CommandLineRunner {

    private final PluginService pluginService;
    private final PluginRepository pluginRepository;

    @Override
    public void run(String... args) {
        List<Plugin> plugins = pluginRepository.findByStatus(Plugin.Status.NORMAL);
        for (Plugin p : plugins) {
            pluginService.recomputeComponents(p);
        }
    }
}
