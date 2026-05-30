package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.StatsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
public class OverviewController {
    @GetMapping("/stats")
    public StatsDto getStats() {
        return new StatsDto(0L, 0L, 0L);
    }
}