package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.ChatPrincipal;
import com.iaihub.toolbox.dto.ChatEventDTO;
import com.iaihub.toolbox.dto.ChatMessageDTO;
import com.iaihub.toolbox.dto.ChatReactionUpdateDTO;
import com.iaihub.toolbox.dto.ChatSendPayload;
import com.iaihub.toolbox.dto.EditPayload;
import com.iaihub.toolbox.dto.ReactionActionPayload;
import com.iaihub.toolbox.dto.RecallPayload;
import com.iaihub.toolbox.dto.TypingEventDTO;
import com.iaihub.toolbox.model.ChatMessage;
import com.iaihub.toolbox.model.ChatReaction;
import com.iaihub.toolbox.repository.ChatMessageRepository;
import com.iaihub.toolbox.repository.ChatReactionRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final long RATE_LIMIT_MS = 2000L;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int DEFAULT_HISTORY_LIMIT = 50;
    private static final long EDIT_WINDOW_MS = 300_000L;   // 5 分钟
    private static final long RECALL_WINDOW_MS = 300_000L; // 5 分钟
    private static final long TYPING_TIMEOUT_MS = 4000L;   // 4 秒无输入自动清除
    private static final int REPLY_PREVIEW_LEN = 80;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatReactionRepository chatReactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<String, Long> lastSendAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> typingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService typingScheduler = Executors.newSingleThreadScheduledExecutor();

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
                .replyTo(payload.getReplyTo())
                .build();

        chatMessageRepository.save(message);
        lastSendAt.put(rateLimitKey, now);

        // 广播给全房间：myReactions 是个人视角字段，不能携带发送者的视角（新消息无 reaction，传空）
        ChatMessageDTO dto = toDTO(message, isGuest,
                reactionCounts(message.getId()),
                List.of(),
                replyDisplayName(message.getReplyTo()),
                replyPreview(message.getReplyTo()));
        messagingTemplate.convertAndSend("/topic/chat." + roomId, dto);
        return Optional.of(dto);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getHistory(String roomId, int limit) {
        return getHistory(roomId, limit, null);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getHistory(String roomId, int limit, String ownerKey) {
        if (roomId == null || roomId.isBlank()) roomId = "global";
        if (limit <= 0 || limit > 200) limit = DEFAULT_HISTORY_LIMIT;

        List<ChatMessage> messages = chatMessageRepository.findRecentByRoomId(roomId, PageRequest.of(0, limit));
        Collections.reverse(messages);

        List<Long> ids = messages.stream().map(ChatMessage::getId).toList();
        Map<Long, Map<String, Integer>> reactionsByMsg = aggregateReactions(ids);

        List<Long> replyIds = messages.stream().map(ChatMessage::getReplyTo)
                .filter(Objects::nonNull).toList();
        Map<Long, ChatMessage> replyById = replyIds.isEmpty() ? Map.of()
                : chatMessageRepository.findByIdIn(replyIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m, (a, b) -> a));

        Map<Long, Set<String>> myByMsg = (ownerKey == null) ? Map.of()
                : myReactions(ids, ownerKey);

        return messages.stream().map(m -> {
            String rd = null, rp = null;
            if (m.getReplyTo() != null) {
                ChatMessage ref = replyById.get(m.getReplyTo());
                if (ref != null) {
                    rd = ref.getDisplayName();
                    rp = preview(ref.getContent());
                }
            }
            return toDTO(m, m.getUserId() == null,
                    reactionsByMsg.getOrDefault(m.getId(), Map.of()),
                    new ArrayList<>(myByMsg.getOrDefault(m.getId(), Set.of())),
                    rd, rp);
        }).toList();
    }

    @Transactional
    public void softDelete(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId).orElse(null);
        if (message == null) return;
        message.setStatus("DELETED");
        message.setDeletedType("ADMIN");
        chatMessageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/chat." + message.getRoomId(),
                ChatEventDTO.builder().type("DELETE").id(messageId).deletedType("ADMIN").build());
    }

    @Transactional
    public void toggleReaction(ChatPrincipal principal, ReactionActionPayload payload) {
        Long messageId = payload.getMessageId();
        String emoji = payload.getEmoji();
        if (emoji == null || emoji.isBlank()) {
            sendErrorToUser(principal, "表情不能为空");
            return;
        }
        ChatMessage msg = chatMessageRepository.findById(messageId).orElse(null);
        if (msg == null || !"ACTIVE".equals(msg.getStatus())) {
            sendErrorToUser(principal, "消息不存在或已删除");
            return;
        }
        String ownerKey = ownerKeyOf(principal);
        if (chatReactionRepository.existsByMessageIdAndOwnerKeyAndEmoji(messageId, ownerKey, emoji)) {
            chatReactionRepository.deleteByMessageIdAndOwnerKeyAndEmoji(messageId, ownerKey, emoji);
        } else {
            chatReactionRepository.save(ChatReaction.builder()
                    .messageId(messageId).ownerKey(ownerKey).emoji(emoji).build());
        }
        ChatReactionUpdateDTO update = ChatReactionUpdateDTO.builder()
                .messageId(messageId)
                .reactions(reactionCounts(messageId))
                .build();
        messagingTemplate.convertAndSend("/topic/chat.reactions." + msg.getRoomId(), update);
    }

    @Transactional
    public void editMessage(ChatPrincipal principal, EditPayload payload) {
        Long id = payload.getId();
        ChatMessage msg = chatMessageRepository.findById(id).orElse(null);
        if (msg == null || !"ACTIVE".equals(msg.getStatus())) {
            sendErrorToUser(principal, "消息不存在或已删除");
            return;
        }
        if (principal.getUserId() == null) {
            sendErrorToUser(principal, "仅登录用户可编辑消息");
            return;
        }
        if (!principal.getUserId().equals(msg.getUserId())) {
            sendErrorToUser(principal, "只能编辑自己的消息");
            return;
        }
        if (Duration.between(msg.getCreatedAt(), LocalDateTime.now()).toMillis() > EDIT_WINDOW_MS) {
            sendErrorToUser(principal, "已超过编辑时限（5 分钟）");
            return;
        }
        String newContent = payload.getContent();
        if (newContent == null || newContent.isBlank()) {
            sendErrorToUser(principal, "内容不能为空");
            return;
        }
        if (newContent.length() > MAX_CONTENT_LENGTH) {
            sendErrorToUser(principal, "内容超过 " + MAX_CONTENT_LENGTH + " 字限制");
            return;
        }
        msg.setContent(XssSanitizer.sanitize(newContent));
        msg.setEdited(true);
        chatMessageRepository.save(msg);

        // 广播给全房间：不携带编辑者的 myReactions 个人视角，观看者的高亮由各端本地维护
        ChatMessageDTO dto = toDTO(msg, false,
                reactionCounts(id),
                List.of(),
                replyDisplayName(msg.getReplyTo()),
                replyPreview(msg.getReplyTo()));
        messagingTemplate.convertAndSend("/topic/chat.edit." + msg.getRoomId(), dto);
    }

    @Transactional
    public void recallMessage(ChatPrincipal principal, RecallPayload payload) {
        Long id = payload.getId();
        ChatMessage msg = chatMessageRepository.findById(id).orElse(null);
        if (msg == null || !"ACTIVE".equals(msg.getStatus())) {
            sendErrorToUser(principal, "消息不存在或已删除");
            return;
        }
        if (principal.getUserId() == null) {
            sendErrorToUser(principal, "仅登录用户可撤回消息");
            return;
        }
        if (!principal.getUserId().equals(msg.getUserId())) {
            sendErrorToUser(principal, "只能撤回自己的消息");
            return;
        }
        if (Duration.between(msg.getCreatedAt(), LocalDateTime.now()).toMillis() > RECALL_WINDOW_MS) {
            sendErrorToUser(principal, "已超过撤回时限（5 分钟）");
            return;
        }
        msg.setStatus("DELETED");
        msg.setDeletedType("SELF");
        chatMessageRepository.save(msg);
        messagingTemplate.convertAndSend("/topic/chat.recall." + msg.getRoomId(),
                ChatEventDTO.builder().type("RECALL").id(id).deletedType("SELF").build());
    }

    public void handleTyping(ChatPrincipal principal, String roomId, boolean isTyping) {
        final String room = (roomId == null || roomId.isBlank()) ? "global" : roomId;
        String key = room + ":" + principal.getName();
        ScheduledFuture<?> existing = typingTasks.get(key);
        if (existing != null) {
            existing.cancel(false);
            typingTasks.remove(key);
        }
        if (isTyping) {
            typingTasks.put(key, typingScheduler.schedule(() -> {
                typingTasks.remove(key);
                broadcastTyping(room, principal, false);
            }, TYPING_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            broadcastTyping(room, principal, true);
        } else {
            broadcastTyping(room, principal, false);
        }
    }

    private void broadcastTyping(String roomId, ChatPrincipal principal, boolean isTyping) {
        messagingTemplate.convertAndSend("/topic/chat.typing." + roomId,
                TypingEventDTO.builder()
                        .roomId(roomId)
                        .userId(principal.getUserId())
                        .displayName(principal.getDisplayName())
                        .isTyping(isTyping)
                        .build());
    }

    private void sendErrorToUser(ChatPrincipal principal, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                ChatEventDTO.builder().type("ERROR").message(errorMessage).build()
        );
    }

    private String ownerKeyOf(ChatPrincipal principal) {
        return principal.getUserId() != null ? principal.getUserId().toString() : principal.getIpHash();
    }

    private Map<String, Integer> reactionCounts(Long messageId) {
        Map<String, Integer> counts = new HashMap<>();
        for (ChatReaction r : chatReactionRepository.findByMessageId(messageId)) {
            counts.merge(r.getEmoji(), 1, Integer::sum);
        }
        return counts;
    }

    private List<String> myReactions(Long messageId, String ownerKey) {
        if (ownerKey == null) return List.of();
        return chatReactionRepository.findByMessageIdAndOwnerKey(messageId, ownerKey).stream()
                .map(ChatReaction::getEmoji).toList();
    }

    private Map<Long, Map<String, Integer>> aggregateReactions(List<Long> ids) {
        Map<Long, Map<String, Integer>> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        for (ChatReaction r : chatReactionRepository.findByMessageIdIn(ids)) {
            result.computeIfAbsent(r.getMessageId(), k -> new HashMap<>()).merge(r.getEmoji(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, Set<String>> myReactions(List<Long> ids, String ownerKey) {
        Map<Long, Set<String>> result = new HashMap<>();
        if (ids.isEmpty() || ownerKey == null) return result;
        for (ChatReaction r : chatReactionRepository.findByMessageIdInAndOwnerKey(ids, ownerKey)) {
            result.computeIfAbsent(r.getMessageId(), k -> new HashSet<>()).add(r.getEmoji());
        }
        return result;
    }

    private String replyDisplayName(Long replyTo) {
        if (replyTo == null) return null;
        return chatMessageRepository.findById(replyTo).map(ChatMessage::getDisplayName).orElse(null);
    }

    private String replyPreview(Long replyTo) {
        if (replyTo == null) return null;
        return chatMessageRepository.findById(replyTo).map(m -> preview(m.getContent())).orElse(null);
    }

    private String preview(String content) {
        if (content == null) return null;
        return content.length() > REPLY_PREVIEW_LEN ? content.substring(0, REPLY_PREVIEW_LEN) + "…" : content;
    }

    private ChatMessageDTO toDTO(ChatMessage m, boolean isGuest,
                                 Map<String, Integer> reactions, List<String> myReactions,
                                 String replyDisplayName, String replyPreview) {
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
                .replyTo(m.getReplyTo())
                .replyToDisplayName(replyDisplayName)
                .replyToContentPreview(replyPreview)
                .edited(m.isEdited())
                .deletedType(m.getDeletedType())
                .reactions(reactions)
                .myReactions(myReactions)
                .build();
    }
}
