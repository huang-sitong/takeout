package com.sky.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端
 *
 * 管理端（商家后台）通过 WebSocket 连接接收实时通知：
 * - 新订单提醒、催单提醒、订单状态变更等
 *
 * 支持多实例部署：配合 RocketMQ 广播模式，每个实例各自推送自己的 WebSocket 客户端
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    /**
     * 会话集合：sid → Session
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        SESSION_MAP.put(sid, session);
        log.info("WebSocket connected: sid={}, total={}", sid, SESSION_MAP.size());
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        SESSION_MAP.remove(sid);
        log.info("WebSocket disconnected: sid={}, total={}", sid, SESSION_MAP.size());
    }

    /**
     * 群发消息给所有已连接的客户端
     *
     * @param message JSON 格式的消息内容
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = SESSION_MAP.values();
        if (sessions.isEmpty()) {
            log.debug("no WebSocket client connected, skip推送: {}", message);
            return;
        }
        for (Session session : sessions) {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("WebSocket 推送失败: sid={}", session.getId(), e);
            }
        }
        log.debug("WebSocket 推送完成: clients={}, message={}", sessions.size(), message);
    }

    /**
     * 获取当前在线客户端数量
     */
    public int getOnlineCount() {
        return SESSION_MAP.size();
    }
}
