package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.PostFavorite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PostFavoriteRepositoryTest {

    @Autowired
    private PostFavoriteRepository repository;

    @Test
    void testSaveFavorite() {
        PostFavorite favorite = PostFavorite.builder()
                .userId(1L)
                .postId(100L)
                .build();
        PostFavorite saved = repository.save(favorite);
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getUserId());
        assertEquals(100L, saved.getPostId());
    }

    @Test
    void testUniqueIndexConstraint() {
        PostFavorite favorite1 = PostFavorite.builder()
                .userId(1L)
                .postId(100L)
                .build();
        repository.save(favorite1);

        PostFavorite favorite2 = PostFavorite.builder()
                .userId(1L)
                .postId(100L)
                .build();
        assertThrows(Exception.class, () -> {
            repository.save(favorite2);
            repository.flush();
        });
    }

    @Test
    void testFindByUserIdAndPostId() {
        PostFavorite favorite = PostFavorite.builder()
                .userId(1L)
                .postId(100L)
                .build();
        repository.save(favorite);

        Optional<PostFavorite> found = repository.findByUserIdAndPostId(1L, 100L);
        assertTrue(found.isPresent());
    }

    @Test
    void testFindByUserId() {
        PostFavorite fav1 = PostFavorite.builder()
                .userId(1L)
                .postId(100L)
                .build();
        repository.save(fav1);

        PostFavorite fav2 = PostFavorite.builder()
                .userId(1L)
                .postId(200L)
                .build();
        repository.save(fav2);

        var favorites = repository.findByUserId(1L);
        assertEquals(2, favorites.size());
    }

    @Test
    void testDeleteByUserIdAndPostId() {
        PostFavorite favorite = PostFavorite.builder()
                .userId(1L)
                .postId(100L)
                .build();
        repository.save(favorite);

        repository.deleteByUserIdAndPostId(1L, 100L);
        Optional<PostFavorite> found = repository.findByUserIdAndPostId(1L, 100L);
        assertFalse(found.isPresent());
    }
}
