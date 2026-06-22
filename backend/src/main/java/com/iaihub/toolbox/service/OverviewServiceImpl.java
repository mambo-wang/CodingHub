package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.dto.ToolRankDto;
import com.iaihub.toolbox.dto.PostRankDto;
import com.iaihub.toolbox.dto.VideoRankDto;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OverviewServiceImpl implements OverviewService {

    private final UserRepository userRepository;
    private final ForumPostRepository forumPostRepository;
    private final ToolRepository toolRepository;
    private final VideoRepository videoRepository;

    public OverviewServiceImpl(UserRepository userRepository,
                               ForumPostRepository forumPostRepository,
                               ToolRepository toolRepository,
                               VideoRepository videoRepository) {
        this.userRepository = userRepository;
        this.forumPostRepository = forumPostRepository;
        this.toolRepository = toolRepository;
        this.videoRepository = videoRepository;
    }

    @Override
    public StatsDto getStats() {
        long userCount = userRepository.count();
        long postCount = forumPostRepository.count();
        long toolCount = toolRepository.count();
        long videoCount = videoRepository.count();
        return new StatsDto(userCount, postCount, toolCount, videoCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ToolRankDto> getToolRanks() {
        return toolRepository.findAll().stream()
                .sorted((a, b) -> {
                    java.math.BigDecimal scoreA = a.getScore() != null ? a.getScore() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal scoreB = b.getScore() != null ? b.getScore() : java.math.BigDecimal.ZERO;
                    return scoreB.compareTo(scoreA);
                })
                .limit(10)
                .map(t -> new ToolRankDto(
                        t.getId(),
                        t.getCategory() != null ? t.getCategory().getName() : "",
                        t.getName(),
                        t.getScore() != null ? t.getScore() : java.math.BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Override
    public List<PostRankDto> getPostRanks() {
        return forumPostRepository.findAll().stream()
                .sorted((a, b) -> {
                    java.math.BigDecimal scoreA = a.getScore() != null ? a.getScore() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal scoreB = b.getScore() != null ? b.getScore() : java.math.BigDecimal.ZERO;
                    return scoreB.compareTo(scoreA);
                })
                .limit(10)
                .map(p -> new PostRankDto(
                        p.getId(),
                        "",
                        p.getTitle(),
                        p.getScore() != null ? p.getScore() : java.math.BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoRankDto> getVideoRanks() {
        return videoRepository.findTop20ByStatusOrderByViewCountDesc(VideoStatus.NORMAL)
                .stream()
                .limit(10)
                .map(v -> new VideoRankDto(
                        v.getId(),
                        v.getTitle(),
                        v.getViewCount() != null ? v.getViewCount() : 0,
                        v.getLikeCount() != null ? v.getLikeCount() : 0))
                .collect(Collectors.toList());
    }
}