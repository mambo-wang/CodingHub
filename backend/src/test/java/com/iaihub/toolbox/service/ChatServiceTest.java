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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatReactionRepository chatReactionRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    private ChatPrincipal loggedInPrincipal;
    private ChatPrincipal guestPrincipal;

    @BeforeEach
    void setUp() {
        loggedInPrincipal = ChatPrincipal.builder()
                .userId(1L)
                .displayName("TestUser")
                .avatarUrl("/avatar.png")
                .ipHash("abc123")
                .admin(false)
                .sessionId("session-1")
                .build();

        guestPrincipal = ChatPrincipal.builder()
                .userId(null)
                .displayName(null)
                .avatarUrl(null)
                .ipHash("guest-hash")
                .admin(false)
                .sessionId("session-guest")
                .build();
    }

    @Test
    void handleMessage_loggedInUser_persistsAndBroadcasts() {
        ChatSendPayload payload = new ChatSendPayload("global", "Hello!", null);
        ChatMessage saved = ChatMessage.builder()
                .id(1L).roomId("global").userId(1L)
                .displayName("TestUser").content("Hello!").status("ACTIVE").build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        Optional<ChatMessageDTO> result = chatService.handleMessage(loggedInPrincipal, payload);

        assertFalse(result.isEmpty());
        assertEquals("Hello!", result.get().getContent());
        assertEquals(1L, result.get().getUserId());
        assertFalse(result.get().isGuest());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.global"), any(ChatMessageDTO.class));
    }

    @Test
    void handleMessage_guestWithNick_persistsAndBroadcasts() {
        ChatSendPayload payload = new ChatSendPayload("global", "Hi!", "GuestNick");
        ChatMessage saved = ChatMessage.builder()
                .id(2L).roomId("global").userId(null)
                .displayName("GuestNick").content("Hi!").status("ACTIVE").build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        Optional<ChatMessageDTO> result = chatService.handleMessage(guestPrincipal, payload);

        assertFalse(result.isEmpty());
        assertTrue(result.get().isGuest());
        assertNull(result.get().getUserId());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void handleMessage_guestWithoutNick_rejects() {
        ChatSendPayload payload = new ChatSendPayload("global", "Hi!", null);

        Optional<ChatMessageDTO> result = chatService.handleMessage(guestPrincipal, payload);

        assertTrue(result.isEmpty());
        verify(chatMessageRepository, never()).save(any());
        verify(messagingTemplate).convertAndSendToUser(eq("guest:session-guest"), eq("/queue/errors"), any());
    }

    @Test
    void handleMessage_blankContent_rejects() {
        ChatSendPayload payload = new ChatSendPayload("global", "   ", null);

        Optional<ChatMessageDTO> result = chatService.handleMessage(loggedInPrincipal, payload);

        assertTrue(result.isEmpty());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void handleMessage_overLength_rejects() {
        String longContent = "a".repeat(1001);
        ChatSendPayload payload = new ChatSendPayload("global", longContent, null);

        Optional<ChatMessageDTO> result = chatService.handleMessage(loggedInPrincipal, payload);

        assertTrue(result.isEmpty());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void handleMessage_xssSanitized() {
        ChatSendPayload payload = new ChatSendPayload("global", "<script>alert(1)</script>", null);
        ChatMessage saved = ChatMessage.builder()
                .id(3L).roomId("global").userId(1L)
                .displayName("TestUser").content("sanitized").status("ACTIVE").build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        Optional<ChatMessageDTO> result = chatService.handleMessage(loggedInPrincipal, payload);

        assertFalse(result.isEmpty());
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertFalse(captor.getValue().getContent().contains("<script>"));
    }

    @Test
    void handleMessage_rateLimit_blocksSecondMessage() {
        ChatSendPayload payload = new ChatSendPayload("global", "First", null);
        ChatMessage saved = ChatMessage.builder()
                .id(4L).roomId("global").userId(1L)
                .displayName("TestUser").content("First").status("ACTIVE").build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        chatService.handleMessage(loggedInPrincipal, payload);

        ChatSendPayload payload2 = new ChatSendPayload("global", "Second", null);
        Optional<ChatMessageDTO> result2 = chatService.handleMessage(loggedInPrincipal, payload2);

        assertTrue(result2.isEmpty());
        verify(chatMessageRepository, times(1)).save(any());
    }

    @Test
    void softDelete_broadcastsDeleteEventWithAdminType() {
        ChatMessage msg = ChatMessage.builder()
                .id(10L).roomId("global").status("ACTIVE").build();
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(msg));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.softDelete(10L);

        verify(chatMessageRepository).save(argThat(m ->
                "DELETED".equals(m.getStatus()) && "ADMIN".equals(m.getDeletedType())));
        ArgumentCaptor<ChatEventDTO> deleteCaptor = ArgumentCaptor.forClass(ChatEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.global"), deleteCaptor.capture());
        ChatEventDTO deleteEv = deleteCaptor.getValue();
        assertEquals("DELETE", deleteEv.getType());
        assertEquals("ADMIN", deleteEv.getDeletedType());
    }

    @Test
    void toggleReaction_addsWhenNotPresent() {
        when(chatReactionRepository.existsByMessageIdAndOwnerKeyAndEmoji(5L, "1", "👍")).thenReturn(false);
        when(chatReactionRepository.findByMessageId(5L)).thenReturn(List.of());

        chatService.toggleReaction(loggedInPrincipal, new ReactionActionPayload(5L, "👍"));

        verify(chatReactionRepository).save(any(ChatReaction.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.reactions.global"), any(ChatReactionUpdateDTO.class));
    }

    @Test
    void toggleReaction_removesWhenPresent() {
        when(chatReactionRepository.existsByMessageIdAndOwnerKeyAndEmoji(5L, "1", "👍")).thenReturn(true);

        chatService.toggleReaction(loggedInPrincipal, new ReactionActionPayload(5L, "👍"));

        verify(chatReactionRepository).deleteByMessageIdAndOwnerKeyAndEmoji(5L, "1", "👍");
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.reactions.global"), any(ChatReactionUpdateDTO.class));
    }

    @Test
    void toggleReaction_rejectsWhenMessageMissing() {
        when(chatReactionRepository.existsByMessageIdAndOwnerKeyAndEmoji(6L, "1", "👍")).thenReturn(false);
        when(chatMessageRepository.findById(6L)).thenReturn(Optional.empty());

        chatService.toggleReaction(loggedInPrincipal, new ReactionActionPayload(6L, "👍"));

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/errors"), any());
        verify(chatReactionRepository, never()).save(any());
    }

    @Test
    void editMessage_authorWithinWindow_updates() {
        ChatMessage msg = ChatMessage.builder().id(7L).roomId("global").userId(1L).displayName("TestUser")
                .content("old").status("ACTIVE").createdAt(LocalDateTime.now()).build();
        when(chatMessageRepository.findById(7L)).thenReturn(Optional.of(msg));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(chatReactionRepository.findByMessageId(7L)).thenReturn(List.of());
        when(chatReactionRepository.findByMessageIdAndOwnerKey(7L, "1")).thenReturn(List.of());

        chatService.editMessage(loggedInPrincipal, new EditPayload(7L, "new content"));

        verify(chatMessageRepository).save(argThat(m -> "new content".equals(m.getContent()) && m.isEdited()));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.edit.global"), any(ChatMessageDTO.class));
    }

    @Test
    void editMessage_nonAuthor_rejects() {
        ChatMessage msg = ChatMessage.builder().id(7L).roomId("global").userId(2L).displayName("Other")
                .content("old").status("ACTIVE").createdAt(LocalDateTime.now()).build();
        when(chatMessageRepository.findById(7L)).thenReturn(Optional.of(msg));

        chatService.editMessage(loggedInPrincipal, new EditPayload(7L, "new"));

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/errors"), any());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void editMessage_expired_rejects() {
        ChatMessage msg = ChatMessage.builder().id(7L).roomId("global").userId(1L).displayName("TestUser")
                .content("old").status("ACTIVE").createdAt(LocalDateTime.now().minusMinutes(10)).build();
        when(chatMessageRepository.findById(7L)).thenReturn(Optional.of(msg));

        chatService.editMessage(loggedInPrincipal, new EditPayload(7L, "new"));

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/errors"), any());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void recallMessage_authorWithinWindow_recalls() {
        ChatMessage msg = ChatMessage.builder().id(8L).roomId("global").userId(1L)
                .status("ACTIVE").createdAt(LocalDateTime.now()).build();
        when(chatMessageRepository.findById(8L)).thenReturn(Optional.of(msg));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        chatService.recallMessage(loggedInPrincipal, new RecallPayload(8L));

        verify(chatMessageRepository).save(argThat(m ->
                "DELETED".equals(m.getStatus()) && "SELF".equals(m.getDeletedType())));
        ArgumentCaptor<ChatEventDTO> recallCaptor = ArgumentCaptor.forClass(ChatEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.recall.global"), recallCaptor.capture());
        ChatEventDTO recallEv = recallCaptor.getValue();
        assertEquals("RECALL", recallEv.getType());
        assertEquals("SELF", recallEv.getDeletedType());
    }

    @Test
    void recallMessage_guest_rejects() {
        ChatMessage msg = ChatMessage.builder().id(8L).userId(1L).status("ACTIVE").createdAt(LocalDateTime.now()).build();
        when(chatMessageRepository.findById(8L)).thenReturn(Optional.of(msg));

        chatService.recallMessage(guestPrincipal, new RecallPayload(8L));

        verify(messagingTemplate).convertAndSendToUser(eq("guest:session-guest"), eq("/queue/errors"), any());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void handleTyping_false_broadcastsClear() {
        chatService.handleTyping(loggedInPrincipal, "global", false);
        ArgumentCaptor<TypingEventDTO> typingCaptor1 = ArgumentCaptor.forClass(TypingEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.typing.global"), typingCaptor1.capture());
        assertFalse(typingCaptor1.getValue().isTyping());
    }

    @Test
    void handleTyping_true_broadcastsTypingThenClears() {
        chatService.handleTyping(loggedInPrincipal, "global", true);
        ArgumentCaptor<TypingEventDTO> typingCaptor2 = ArgumentCaptor.forClass(TypingEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.typing.global"), typingCaptor2.capture());
        assertTrue(typingCaptor2.getValue().isTyping());
        // 取消挂起的超时任务，避免测试线程泄漏
        chatService.handleTyping(loggedInPrincipal, "global", false);
    }
}
