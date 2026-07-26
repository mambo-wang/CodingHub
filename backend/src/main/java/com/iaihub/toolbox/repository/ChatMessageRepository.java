package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId AND m.status = 'ACTIVE' " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentByRoomId(@Param("roomId") String roomId,
                                         org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = 'DELETED' WHERE m.id = :id")
    int softDeleteById(@Param("id") Long id);
}
