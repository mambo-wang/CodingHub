package com.iaihub.toolbox.service.tag;

import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import com.iaihub.toolbox.repository.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    @Transactional
    public TagDTO createTag(String name, String type) {
        TagType tagType = TagType.valueOf(type.toUpperCase());
        return tagRepository.findByNameAndTagType(name, tagType)
                .map(this::toDTO)
                .orElseGet(() -> {
                    Tag tag = Tag.builder()
                            .name(name)
                            .tagType(tagType)
                            .usageCount(0)
                            .build();
                    tag = tagRepository.save(tag);
                    return toDTO(tag);
                });
    }

    @Transactional(readOnly = true)
    public List<TagDTO> getTagsByType(String type) {
        TagType tagType = TagType.valueOf(type.toUpperCase());
        return tagRepository.findByTagType(tagType).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagDTO> getHotTags(String type, int limit) {
        TagType tagType = TagType.valueOf(type.toUpperCase());
        int cappedLimit = Math.min(limit, 100);
        return tagRepository.findTopByTagTypeOrderByUsageCountDesc(tagType, PageRequest.of(0, cappedLimit))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public Tag getOrCreateTag(String name, TagType tagType) {
        return tagRepository.findByNameAndTagType(name, tagType)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .name(name)
                        .tagType(tagType)
                        .usageCount(0)
                        .build()));
    }

    /**
     * 批量解析标签名列表为标签 ID 列表。
     * 按名称查找已有标签，不存在的标签自动创建。
     * 处理并发场景：捕获唯一约束冲突后回退查询。
     */
    @Transactional
    public List<Long> resolveOrCreateTags(List<String> names, TagType tagType) {
        List<Long> tagIds = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            Tag tag = tagRepository.findByNameAndTagType(name, tagType)
                    .orElseGet(() -> {
                        try {
                            return tagRepository.save(Tag.builder()
                                    .name(name)
                                    .tagType(tagType)
                                    .usageCount(0)
                                    .build());
                        } catch (DataIntegrityViolationException e) {
                            // 并发创建场景：回退查询已有记录
                            logger.warn("Concurrent tag creation for name={}, type={}, falling back to query", name, tagType);
                            return tagRepository.findByNameAndTagType(name, tagType)
                                    .orElseThrow(() -> new RuntimeException("标签创建失败: " + name));
                        }
                    });
            tagIds.add(tag.getId());
        }
        return tagIds;
    }

    @Transactional
    public void incrementUsage(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("标签不存在: " + tagId));
        tag.incrementUsage();
        tagRepository.save(tag);
    }

    @Transactional
    public void decrementUsage(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("标签不存在: " + tagId));
        tag.decrementUsage();
        tagRepository.save(tag);
    }

    @Transactional(readOnly = true)
    public TagDTO getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("标签不存在: " + id));
        return toDTO(tag);
    }

    public TagDTO toDTO(Tag tag) {
        return new TagDTO(tag.getId(), tag.getName(), tag.getTagType().name(), tag.getUsageCount());
    }
}
