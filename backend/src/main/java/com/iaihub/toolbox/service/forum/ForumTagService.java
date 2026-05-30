package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumTagDTO;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.model.forum.ForumTag;
import com.iaihub.toolbox.repository.forum.ForumTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumTagService {

    private final ForumTagRepository tagRepository;

    public List<ForumTagDTO> getAllTags() {
        return tagRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    public List<ForumTagDTO> getHotTags() {
        return tagRepository.findTop10ByOrderByPostCountDesc().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public ForumTagDTO createTag(String name, boolean isSystem) {
        if (tagRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("标签已存在: " + name);
        }

        ForumTag tag = new ForumTag();
        tag.setName(name);
        tag.setIsSystem(isSystem);
        tag = tagRepository.save(tag);

        return toDTO(tag);
    }

    private ForumTagDTO toDTO(ForumTag tag) {
        return new ForumTagDTO(tag.getId(), tag.getName(), tag.getPostCount(), tag.getIsSystem());
    }
}