package com.iaihub.toolbox.service.tag;

import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import com.iaihub.toolbox.repository.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceResolveTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag buildTag(Long id, String name, TagType type) {
        return Tag.builder().id(id).name(name).tagType(type).usageCount(0).build();
    }

    @Test
    void resolveOrCreateTags_allExisting() {
        Tag cli = buildTag(1L, "CLI", TagType.TOOL);
        Tag python = buildTag(2L, "Python", TagType.TOOL);

        when(tagRepository.findByNameAndTagType("CLI", TagType.TOOL)).thenReturn(Optional.of(cli));
        when(tagRepository.findByNameAndTagType("Python", TagType.TOOL)).thenReturn(Optional.of(python));

        List<Long> ids = tagService.resolveOrCreateTags(List.of("CLI", "Python"), TagType.TOOL);

        assertEquals(List.of(1L, 2L), ids);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void resolveOrCreateTags_allNew() {
        when(tagRepository.findByNameAndTagType("Rust", TagType.TOOL)).thenReturn(Optional.empty());
        when(tagRepository.findByNameAndTagType("Go", TagType.TOOL)).thenReturn(Optional.empty());

        Tag rust = buildTag(10L, "Rust", TagType.TOOL);
        Tag go = buildTag(11L, "Go", TagType.TOOL);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            if ("Rust".equals(tag.getName())) return rust;
            if ("Go".equals(tag.getName())) return go;
            return tag;
        });

        List<Long> ids = tagService.resolveOrCreateTags(List.of("Rust", "Go"), TagType.TOOL);

        assertEquals(List.of(10L, 11L), ids);
        verify(tagRepository, times(2)).save(any(Tag.class));
    }

    @Test
    void resolveOrCreateTags_mixedExistingAndNew() {
        Tag cli = buildTag(1L, "CLI", TagType.TOOL);
        when(tagRepository.findByNameAndTagType("CLI", TagType.TOOL)).thenReturn(Optional.of(cli));
        when(tagRepository.findByNameAndTagType("Go", TagType.TOOL)).thenReturn(Optional.empty());

        Tag go = buildTag(5L, "Go", TagType.TOOL);
        when(tagRepository.save(any(Tag.class))).thenReturn(go);

        List<Long> ids = tagService.resolveOrCreateTags(List.of("CLI", "Go"), TagType.TOOL);

        assertEquals(List.of(1L, 5L), ids);
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void resolveOrCreateTags_emptyList() {
        List<Long> ids = tagService.resolveOrCreateTags(List.of(), TagType.TOOL);

        assertTrue(ids.isEmpty());
        verify(tagRepository, never()).findByNameAndTagType(anyString(), any());
        verify(tagRepository, never()).save(any());
    }

    @Test
    void resolveOrCreateTags_skipsBlankNames() {
        Tag cli = buildTag(1L, "CLI", TagType.TOOL);
        when(tagRepository.findByNameAndTagType("CLI", TagType.TOOL)).thenReturn(Optional.of(cli));

        List<Long> ids = tagService.resolveOrCreateTags(Arrays.asList("", "CLI", null), TagType.TOOL);

        assertEquals(List.of(1L), ids);
    }

    @Test
    void resolveOrCreateTags_concurrentCreationFallback() {
        // First call returns empty (not found)
        when(tagRepository.findByNameAndTagType("NewLang", TagType.TOOL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(buildTag(20L, "NewLang", TagType.TOOL)));

        // save() throws DataIntegrityViolationException (concurrent insert)
        when(tagRepository.save(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        List<Long> ids = tagService.resolveOrCreateTags(List.of("NewLang"), TagType.TOOL);

        assertEquals(List.of(20L), ids);
        // findByNameAndTagType called twice: initial + fallback after conflict
        verify(tagRepository, times(2)).findByNameAndTagType("NewLang", TagType.TOOL);
    }
}
