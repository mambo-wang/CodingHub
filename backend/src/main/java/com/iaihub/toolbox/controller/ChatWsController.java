package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.config.ChatPrincipal;
import com.iaihub.toolbox.dto.EditPayload;
import com.iaihub.toolbox.dto.ReactionActionPayload;
import com.iaihub.toolbox.dto.RecallPayload;
import com.iaihub.toolbox.dto.TypingEventDTO;
import com.iaihub.toolbox.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void handleMessage(com.iaihub.toolbox.dto.ChatSendPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        ChatPrincipal principal = resolvePrincipal(headerAccessor);
        if (principal == null) return;
        chatService.handleMessage(principal, payload);
    }

    @MessageMapping("/chat.react")
    public void handleReact(ReactionActionPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        ChatPrincipal principal = resolvePrincipal(headerAccessor);
        if (principal == null) return;
        chatService.toggleReaction(principal, payload);
    }

    @MessageMapping("/chat.edit")
    public void handleEdit(EditPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        ChatPrincipal principal = resolvePrincipal(headerAccessor);
        if (principal == null) return;
        chatService.editMessage(principal, payload);
    }

    @MessageMapping("/chat.recall")
    public void handleRecall(RecallPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        ChatPrincipal principal = resolvePrincipal(headerAccessor);
        if (principal == null) return;
        chatService.recallMessage(principal, payload);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(TypingEventDTO payload, SimpMessageHeaderAccessor headerAccessor) {
        ChatPrincipal principal = resolvePrincipal(headerAccessor);
        if (principal == null) return;
        chatService.handleTyping(principal, payload.getRoomId(), payload.isTyping());
    }

    private ChatPrincipal resolvePrincipal(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) return null;
        ChatPrincipal principal = (ChatPrincipal) sessionAttributes.get("principal");
        if (principal == null) {
            log.warn("WebSocket message received without principal");
        }
        return principal;
    }
}
