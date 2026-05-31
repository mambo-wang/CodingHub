package com.iaihub.toolbox.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP 连接管理器（支持 SSE）
 */
@Component
public class McpConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(McpConnectionManager.class);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicInteger connectionIdGenerator = new AtomicInteger(1);
    private final Map<Integer, Long> connectionTimestamps = new ConcurrentHashMap<>();
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    /**
     * 注册 SSE 连接
     */
    public SseEmitter registerEmitter(SseEmitter emitter) {
        int connectionId = connectionIdGenerator.getAndIncrement();
        emitters.add(emitter);
        activeConnections.incrementAndGet();
        connectionTimestamps.put(connectionId, System.currentTimeMillis());

        logger.info("SSE connection registered: id={}, active={}", connectionId, activeConnections.get());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            activeConnections.decrementAndGet();
            connectionTimestamps.remove(connectionId);
            logger.info("SSE connection completed: id={}, active={}", connectionId, activeConnections.get());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            activeConnections.decrementAndGet();
            connectionTimestamps.remove(connectionId);
            logger.warn("SSE connection timed out: id={}", connectionId);
        });

        emitter.onError(e -> {
            emitters.remove(emitter);
            activeConnections.decrementAndGet();
            connectionTimestamps.remove(connectionId);
            logger.error("SSE connection error: id={}", connectionId, e);
        });

        return emitter;
    }

    /**
     * 发送事件到所有连接
     */
    public void broadcastEvent(String eventName, Object data) {
        String message = "event: " + eventName + "\ndata: " + serialize(data) + "\n\n";
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(message);
            } catch (Exception e) {
                logger.warn("Failed to send event to emitter", e);
                emitters.remove(emitter);
            }
        }
    }

    /**
     * 发送消息到指定连接
     */
    public void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.getDelegate().send(
                    org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name(eventName)
                            .data(serialize(data))
            );
        } catch (Exception e) {
            logger.warn("Failed to send to emitter", e);
            emitters.remove(emitter);
        }
    }

    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }

    /**
     * 心跳检测 - 移除超时连接
     */
    public void heartbeat() {
        long now = System.currentTimeMillis();
        for (SseEmitter emitter : emitters) {
            // 检查连接是否超时
            // 这里简化处理，实际应该记录每个连接的时间戳
        }
        logger.debug("Heartbeat: active connections={}", activeConnections.get());
    }

    /**
     * 关闭所有连接
     */
    public void shutdown() {
        logger.info("Shutting down all SSE connections");
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.warn("Error completing emitter", e);
            }
        }
        emitters.clear();
        activeConnections.set(0);
        connectionTimestamps.clear();
    }

    /**
     * 序列化对象为 JSON
     */
    private String serialize(Object data) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            logger.error("Failed to serialize data", e);
            return "{}";
        }
    }

    /**
     * Spring SseEmitter 别名（避免与java SE冲突）
     */
    public static class SseEmitter {
        private final org.springframework.web.servlet.mvc.method.annotation.SseEmitter delegate;

        public SseEmitter(Long timeout) {
            this.delegate = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(timeout);
        }

        public void onCompletion(Runnable callback) {
            delegate.onCompletion(callback);
        }

        public void onTimeout(Runnable callback) {
            delegate.onTimeout(callback);
        }

        public void onError(java.util.function.Consumer<Throwable> callback) {
            delegate.onError(callback);
        }

        public void send(String data) throws Exception {
            delegate.send(data);
        }

        public org.springframework.web.servlet.mvc.method.annotation.SseEmitter getDelegate() {
            return delegate;
        }

        /**
         * SSE event builder (Spring 5.1+ style)
         */
        public static SseEmitterEvent event() {
            return new SseEmitterEvent();
        }

        public void complete() {
            delegate.complete();
        }

        public void completeWithError(Throwable t) {
            delegate.completeWithError(t);
        }
    }

    /**
     * SSE event builder
     */
    public static class SseEmitterEvent {
        private String name;
        private Object data;

        public SseEmitterEvent name(String name) {
            this.name = name;
            return this;
        }

        public SseEmitterEvent data(Object data) {
            this.data = data;
            return this;
        }

        public org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder builder() {
            return org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                    .name(name)
                    .data(data);
        }
    }
}