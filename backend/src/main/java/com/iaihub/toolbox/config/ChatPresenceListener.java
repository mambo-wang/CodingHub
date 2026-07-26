package com.iaihub.toolbox.config;

import com.iaihub.toolbox.dto.ChatEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatPresenceListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> onlineSessions = ConcurrentHashMap.newKeySet();

    public ChatPresenceListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId == null) return;
        onlineSessions.add(sessionId);
        broadcastPresence();
        log.debug("Chat session connected: {}, online={}", sessionId, onlineSessions.size());
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            onlineSessions.remove(sessionId);
        }
        broadcastPresence();
        log.debug("Chat session disconnected: {}, online={}", sessionId, onlineSessions.size());
    }

    private void broadcastPresence() {
        messagingTemplate.convertAndSend("/topic/chat.presence",
                ChatEventDTO.builder().type("PRESENCE").online(onlineSessions.size()).build());
    }
}
