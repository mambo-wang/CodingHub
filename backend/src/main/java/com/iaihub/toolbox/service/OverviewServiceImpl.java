package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import org.springframework.stereotype.Service;

@Service
public class OverviewServiceImpl implements OverviewService {

    private final UserRepository userRepository;
    private final ForumPostRepository forumPostRepository;
    private final com.iaihub.toolbox.repository.ToolRepository toolRepository;

    public OverviewServiceImpl(UserRepository userRepository,
                               ForumPostRepository forumPostRepository,
                               com.iaihub.toolbox.repository.ToolRepository toolRepository) {
        this.userRepository = userRepository;
        this.forumPostRepository = forumPostRepository;
        this.toolRepository = toolRepository;
    }

    @Override
    public StatsDto getStats() {
        long userCount = userRepository.count();
        long postCount = forumPostRepository.count();
        long toolCount = toolRepository.count();
        return new StatsDto(userCount, postCount, toolCount);
    }
}