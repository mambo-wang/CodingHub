package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.config.ChatPrincipal;
import com.iaihub.toolbox.dto.ChatSendPayload;
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
    public void handleMessage(ChatSendPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        ChatPrincipal principal = (ChatPrincipal) sessionAttributes.get("principal");
        if (principal == null) {
            log.warn("WebSocket message received without principal");
            return;
        }

        chatService.handleMessage(principal, payload);
    }
}
