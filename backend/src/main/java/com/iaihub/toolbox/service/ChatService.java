package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.ChatPrincipal;
import com.iaihub.toolbox.dto.ChatEventDTO;
import com.iaihub.toolbox.dto.ChatMessageDTO;
import com.iaihub.toolbox.dto.ChatSendPayload;
import com.iaihub.toolbox.model.ChatMessage;
import com.iaihub.toolbox.repository.ChatMessageRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final long RATE_LIMIT_MS = 2000L;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<String, Long> lastSendAt = new ConcurrentHashMap<>();

    public Optional<ChatMessageDTO> handleMessage(ChatPrincipal principal, ChatSendPayload payload) {
        String rateLimitKey = principal.getUserId() != null
                ? "u:" + principal.getUserId()
                : "ip:" + principal.getIpHash();

        long now = System.currentTimeMillis();
        Long lastTime = lastSendAt.get(rateLimitKey);
        if (lastTime != null && (now - lastTime) < RATE_LIMIT_MS) {
            sendErrorToUser(principal, "发送过于频繁，请稍后再试");
            return Optional.empty();
        }

        String content = payload.getContent();
        if (content == null || content.isBlank()) {
            sendErrorToUser(principal, "消息内容不能为空");
            return Optional.empty();
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            sendErrorToUser(principal, "消息内容不能超过" + MAX_CONTENT_LENGTH + "字");
            return Optional.empty();
        }

        String displayName;
        Long userId;
        String avatarUrl;
        boolean isGuest;

        if (principal.getUserId() != null) {
            userId = principal.getUserId();
            displayName = principal.getDisplayName();
            avatarUrl = principal.getAvatarUrl();
            isGuest = false;
        } else {
            userId = null;
            String payloadNick = payload.getDisplayName();
            if (payloadNick == null || payloadNick.isBlank()) {
                sendErrorToUser(principal, "游客必须提供昵称");
                return Optional.empty();
            }
            displayName = XssSanitizer.sanitize(payloadNick);
            avatarUrl = null;
            isGuest = true;
        }

        String sanitizedContent = XssSanitizer.sanitize(content);
        String roomId = payload.getRoomId() != null && !payload.getRoomId().isBlank()
                ? payload.getRoomId() : "global";

        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .userId(userId)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .content(sanitizedContent)
                .status("ACTIVE")
                .build();

        chatMessageRepository.save(message);
        lastSendAt.put(rateLimitKey, now);

        ChatMessageDTO dto = toDTO(message, isGuest);
        messagingTemplate.convertAndSend("/topic/chat." + roomId, dto);
        return Optional.of(dto);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getHistory(String roomId, int limit) {
        if (roomId == null || roomId.isBlank()) roomId = "global";
        if (limit <= 0 || limit > 200) limit = DEFAULT_HISTORY_LIMIT;

        List<ChatMessage> messages = chatMessageRepository.findRecentByRoomId(roomId, PageRequest.of(0, limit));
        Collections.reverse(messages);
        return messages.stream().map(m -> toDTO(m, m.getUserId() == null)).toList();
    }

    @Transactional
    public void softDelete(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId).orElse(null);
        if (message == null) return;
        chatMessageRepository.softDeleteById(messageId);
        messagingTemplate.convertAndSend("/topic/chat." + message.getRoomId(),
                ChatEventDTO.builder().type("DELETE").id(messageId).build());
    }

    private void sendErrorToUser(ChatPrincipal principal, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                ChatEventDTO.builder().type("ERROR").message(errorMessage).build()
        );
    }

    private ChatMessageDTO toDTO(ChatMessage m, boolean isGuest) {
        return ChatMessageDTO.builder()
                .id(m.getId())
                .roomId(m.getRoomId())
                .userId(m.getUserId())
                .displayName(m.getDisplayName())
                .avatarUrl(m.getAvatarUrl())
                .content(m.getContent())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .guest(isGuest)
                .build();
    }
}
