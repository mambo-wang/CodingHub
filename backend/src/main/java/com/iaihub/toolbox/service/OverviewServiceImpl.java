package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.dto.ToolRankDto;
import com.iaihub.toolbox.dto.PostRankDto;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.forum.ForumCategory;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.forum.ForumCategoryRepository;
import com.iaihub.toolbox.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final ToolRepository toolRepository;

    public OverviewServiceImpl(UserRepository userRepository,
                               ForumPostRepository forumPostRepository,
                               ForumCategoryRepository forumCategoryRepository,
                               CategoryRepository categoryRepository,
                               ToolRepository toolRepository) {
        this.userRepository = userRepository;
        this.forumPostRepository = forumPostRepository;
        this.forumCategoryRepository = forumCategoryRepository;
        this.categoryRepository = categoryRepository;
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
        // 使用工具分类 CategoryRepository
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            return result;
        }

        Map<Long, List<Tool>> toolsByCategory = toolRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory().getId() : 0L));

        for (Category category : categories) {
            List<Tool> tools = toolsByCategory.getOrDefault(category.getId(), Collections.emptyList());
            tools.stream()
                    // 按 score 降序排列
                    .sorted((a, b) -> {
                        java.math.BigDecimal scoreA = a.getScore() != null ? a.getScore() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal scoreB = b.getScore() != null ? b.getScore() : java.math.BigDecimal.ZERO;
                        return scoreB.compareTo(scoreA);
                    })
                    .limit(5)
                    .forEach(t -> result.add(new ToolRankDto(
                            t.getId(),
                            category.getName(),
                            t.getName(),
                            t.getScore() != null ? t.getScore() : java.math.BigDecimal.ZERO)));
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
                    // 按 score 降序排列
                    .sorted((a, b) -> {
                        java.math.BigDecimal scoreA = a.getScore() != null ? a.getScore() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal scoreB = b.getScore() != null ? b.getScore() : java.math.BigDecimal.ZERO;
                        return scoreB.compareTo(scoreA);
                    })
                    .limit(5)
                    .forEach(p -> result.add(new PostRankDto(
                            p.getId(),
                            category.getName(),
                            p.getTitle(),
                            p.getScore() != null ? p.getScore() : java.math.BigDecimal.ZERO)));
        }
        return result;
    }
}