package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.dto.ToolRankDto;
import com.iaihub.toolbox.dto.PostRankDto;
import com.iaihub.toolbox.model.forum.ForumCategory;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.forum.ForumCategoryRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OverviewServiceImpl implements OverviewService {

    private final UserRepository userRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumCategoryRepository forumCategoryRepository;
    private final ToolRepository toolRepository;

    public OverviewServiceImpl(UserRepository userRepository,
                               ForumPostRepository forumPostRepository,
                               ForumCategoryRepository forumCategoryRepository,
                               ToolRepository toolRepository) {
        this.userRepository = userRepository;
        this.forumPostRepository = forumPostRepository;
        this.forumCategoryRepository = forumCategoryRepository;
        this.toolRepository = toolRepository;
    }

    @Override
    public StatsDto getStats() {
        long userCount = userRepository.count();
        long postCount = forumPostRepository.count();
        long toolCount = toolRepository.count();
        return new StatsDto(userCount, postCount, toolCount);
    }

    @Override
    public List<ToolRankDto> getToolRanks() {
        List<ToolRankDto> result = new ArrayList<>();
        List<ForumCategory> categories = forumCategoryRepository.findAll();
        if (categories.isEmpty()) {
            return result;
        }

        Map<Long, List<Tool>> toolsByCategory = toolRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory().getId() : 0L));

        for (ForumCategory category : categories) {
            List<Tool> tools = toolsByCategory.getOrDefault(category.getId(), Collections.emptyList());
            tools.stream()
                    .sorted((a, b) -> Long.compare(
                            b.getCreatedAt() != null ? b.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : 0,
                            a.getCreatedAt() != null ? a.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : 0))
                    .limit(5)
                    .forEach(t -> result.add(new ToolRankDto(
                            category.getName(),
                            t.getName(),
                            1L)));
        }
        return result;
    }

    @Override
    public List<PostRankDto> getPostRanks() {
        List<PostRankDto> result = new ArrayList<>();
        List<ForumCategory> categories = forumCategoryRepository.findAll();
        if (categories.isEmpty()) {
            return result;
        }

        Map<Long, List<ForumPost>> postsByCategory =
            forumPostRepository.findAll().stream()
                .collect(Collectors.groupingBy(p -> p.getCategoryId() != null ? p.getCategoryId() : 0L));

        for (ForumCategory category : categories) {
            List<ForumPost> posts = postsByCategory.getOrDefault(category.getId(), Collections.emptyList());
            posts.stream()
                    .sorted((a, b) -> Integer.compare(
                            b.getCommentCount() != null ? b.getCommentCount() : 0,
                            a.getCommentCount() != null ? a.getCommentCount() : 0))
                    .limit(5)
                    .forEach(p -> result.add(new PostRankDto(
                            category.getName(),
                            p.getTitle(),
                            (long) (p.getCommentCount() != null ? p.getCommentCount() : 0))));
        }
        return result;
    }
}