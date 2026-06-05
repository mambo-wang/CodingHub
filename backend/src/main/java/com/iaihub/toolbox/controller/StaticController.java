package com.iaihub.toolbox.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
public class StaticController {

    @GetMapping(value = "/readme", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String getReadme() throws IOException {
        Resource resource = new ClassPathResource("static/README.md");
        if (resource.exists()) {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        }
        // Fallback to root README if not found in resources
        try {
            java.io.File rootReadme = new java.io.File(System.getProperty("user.home"), "../repos/iaihub/README.md");
            if (rootReadme.exists()) {
                return java.nio.file.Files.readString(rootReadme.toPath(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "# 项目 README\n\nREADME 文件未找到";
    }
}
