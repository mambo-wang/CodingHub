package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ChatMessageDTO;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "global") String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessageDTO> messages = chatService.getHistory(roomId, limit);
        return ResponseEntity.ok(Map.of("code", 200, "data", messages));
    }

    @DeleteMapping("/messages/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        chatService.softDelete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "deleted"));
    }
}
