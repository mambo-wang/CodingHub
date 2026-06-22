package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.dto.ToolRankDto;
import com.iaihub.toolbox.dto.PostRankDto;
import com.iaihub.toolbox.dto.VideoRankDto;

import java.util.List;

public interface OverviewService {
    StatsDto getStats();
    List<ToolRankDto> getToolRanks();
    List<PostRankDto> getPostRanks();
    List<VideoRankDto> getVideoRanks();
}