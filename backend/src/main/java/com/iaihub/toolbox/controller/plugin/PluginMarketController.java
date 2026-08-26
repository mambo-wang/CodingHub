package com.iaihub.toolbox.controller.plugin;

import com.iaihub.toolbox.service.plugin.PluginService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * CodeBuddy 兼容市场目录端点：marketplace.json 由插件表实时聚合生成（ADR-0002 决策）。
 */
@RestController
@RequestMapping("/api/v1/plugin-market")
@RequiredArgsConstructor
public class PluginMarketController {

    private final PluginService pluginService;

    @GetMapping(value = "/marketplace.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> marketplace() {
        return ResponseEntity.ok(pluginService.getMarketplace());
    }

    // zip 市场：marketplace.json + 各插件源码目录，CodeBuddy 直接通过 HTTP 下载该 zip 作为市场
    @GetMapping("/marketplace.zip")
    public void marketplaceZip(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=marketplace.zip");
        pluginService.downloadMarketplaceZip(response.getOutputStream());
    }
}
