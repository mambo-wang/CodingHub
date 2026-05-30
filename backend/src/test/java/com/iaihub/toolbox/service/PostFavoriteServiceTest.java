package com.iaihub.toolbox.service;

import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.repository.PostFavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostFavoriteServiceTest {

    @Mock
    private PostFavoriteRepository repository;

    @InjectMocks
    private PostFavoriteService service;

    private Long userId = 1L;
    private Long postId = 100L;

    @Test
    void testAddFavorite_Success() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        when(repository.save(any(PostFavorite.class))).thenAnswer(inv -> {
            PostFavorite fav = inv.getArgument(0);
            fav.setId(1L);
            return fav;
        });

        PostFavorite result = service.addFavorite(userId, postId);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(repository).save(any(PostFavorite.class));
    }

    @Test
    void testAddFavorite_AlreadyFavorited() {
        PostFavorite existing = PostFavorite.builder().id(1L).userId(userId).postId(postId).build();
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        PostFavorite result = service.addFavorite(userId, postId);
        assertEquals(existing.getId(), result.getId());
        verify(repository, never()).save(any(PostFavorite.class));
    }

    @Test
    void testRemoveFavorite_Success() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(PostFavorite.builder().build()));
        doNothing().when(repository).deleteByUserIdAndPostId(userId, postId);

        boolean result = service.removeFavorite(userId, postId);
        assertTrue(result);
        verify(repository).deleteByUserIdAndPostId(userId, postId);
    }

    @Test
    void testRemoveFavorite_NotFound() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        boolean result = service.removeFavorite(userId, postId);
        assertFalse(result);
    }

    @Test
    void testGetUserFavorites() {
        PostFavorite fav1 = PostFavorite.builder().id(1L).userId(userId).postId(100L).build();
        PostFavorite fav2 = PostFavorite.builder().id(2L).userId(userId).postId(200L).build();
        when(repository.findByUserId(userId)).thenReturn(Arrays.asList(fav1, fav2));

        List<PostFavorite> result = service.getUserFavorites(userId);
        assertEquals(2, result.size());
    }

    @Test
    void testIsFavorited() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(PostFavorite.builder().build()));
        assertTrue(service.isFavorited(userId, postId));
    }

    @Test
    void testIsNotFavorited() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        assertFalse(service.isFavorited(userId, postId));
    }
}
