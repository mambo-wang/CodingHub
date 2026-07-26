package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatReactionRepository extends JpaRepository<ChatReaction, Long> {

    List<ChatReaction> findByMessageId(Long messageId);

    List<ChatReaction> findByMessageIdIn(List<Long> messageIds);

    List<ChatReaction> findByMessageIdAndOwnerKey(Long messageId, String ownerKey);

    List<ChatReaction> findByMessageIdInAndOwnerKey(List<Long> messageIds, String ownerKey);

    boolean existsByMessageIdAndOwnerKeyAndEmoji(Long messageId, String ownerKey, String emoji);

    @Modifying
    @Query("DELETE FROM ChatReaction r WHERE r.messageId = :messageId AND r.ownerKey = :ownerKey AND r.emoji = :emoji")
    void deleteByMessageIdAndOwnerKeyAndEmoji(@Param("messageId") Long messageId,
                                              @Param("ownerKey") String ownerKey,
                                              @Param("emoji") String emoji);
}
