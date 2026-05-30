package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.service.OverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/stats")
    public StatsDto getStats() {
        return overviewService.getStats();
    }
}