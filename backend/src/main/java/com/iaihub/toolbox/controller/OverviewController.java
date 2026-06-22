package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.dto.ToolRankDto;
import com.iaihub.toolbox.dto.PostRankDto;
import com.iaihub.toolbox.dto.VideoRankDto;
import com.iaihub.toolbox.service.OverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/tool-ranks")
    public List<ToolRankDto> getToolRanks() {
        return overviewService.getToolRanks();
    }

    @GetMapping("/post-ranks")
    public List<PostRankDto> getPostRanks() {
        return overviewService.getPostRanks();
    }

    @GetMapping("/video-ranks")
    public List<VideoRankDto> getVideoRanks() {
        return overviewService.getVideoRanks();
    }
}