package com.iaihub.toolbox.config;

import com.iaihub.toolbox.dto.ChatEventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatPresenceListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatPresenceListener listener;

    private Message<byte[]> connectMessage(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
        accessor.setSessionId(sessionId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> disconnectMessage(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId(sessionId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connectThenDisconnect_updatesOnlineCount() {
        listener.handleSessionConnected(new SessionConnectEvent(this, connectMessage("s1")));
        ArgumentCaptor<ChatEventDTO> first = ArgumentCaptor.forClass(ChatEventDTO.class);
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/chat.presence"), first.capture());
        assertEquals(1, first.getValue().getOnline());

        listener.handleSessionDisconnected(new SessionDisconnectEvent(this, disconnectMessage("s1"), "s1", CloseStatus.NORMAL));
        ArgumentCaptor<ChatEventDTO> last = ArgumentCaptor.forClass(ChatEventDTO.class);
        verify(messagingTemplate, atLeast(2)).convertAndSend(eq("/topic/chat.presence"), last.capture());
        assertEquals(0, last.getValue().getOnline());
    }

    @Test
    void multipleSessions_countDistinctConnections() {
        listener.handleSessionConnected(new SessionConnectEvent(this, connectMessage("a1")));
        listener.handleSessionConnected(new SessionConnectEvent(this, connectMessage("a2")));

        ArgumentCaptor<ChatEventDTO> count = ArgumentCaptor.forClass(ChatEventDTO.class);
        verify(messagingTemplate, atLeast(2)).convertAndSend(eq("/topic/chat.presence"), count.capture());
        assertEquals(2, count.getValue().getOnline());
    }
}
