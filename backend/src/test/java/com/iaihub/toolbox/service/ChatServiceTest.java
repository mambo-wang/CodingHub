package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.ChatPrincipal;
import com.iaihub.toolbox.dto.ChatEventDTO;
import com.iaihub.toolbox.dto.ChatMessageDTO;
import com.iaihub.toolbox.dto.ChatSendPayload;
import com.iaihub.toolbox.model.ChatMessage;
import com.iaihub.toolbox.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

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
    void softDelete_broadcastsDeleteEvent() {
        ChatMessage msg = ChatMessage.builder()
                .id(10L).roomId("global").status("ACTIVE").build();
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(msg));

        chatService.softDelete(10L);

        verify(chatMessageRepository).softDeleteById(10L);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat.global"), any(ChatEventDTO.class));
    }
}
