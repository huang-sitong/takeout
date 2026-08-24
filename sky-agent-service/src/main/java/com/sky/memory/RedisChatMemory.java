package com.sky.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis 的多轮会话记忆（Phase C）—— 实现 Spring AI {@link ChatMemory} 接口
 *
 * 设计要点：
 * - agent-service 无 DB 原则：会话历史存 Redis List（agent:chat:memory:{conversationId}）
 * - conversationId 即 userId：一个用户一个持续会话（前端做多会话时可扩展为 sessionId）
 * - 只持久化 UserMessage / AssistantMessage 的纯文本——System Prompt 每次请求显式注入，
 *   工具调用中间消息（toolCalls/ToolResponse）不入库：反序列化复杂且对点餐场景无价值，
 *   assistant 的自然语言总结已足够 LLM 理解上下文，还省 token
 * - 滑动窗口 MAX_MESSAGES 条 + TTL 过期自动清理
 *
 * 配合 MessageChatMemoryAdvisor 使用：before 时读历史拼入 prompt，
 * after 时将本轮 user/assistant 写回（经 .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ...)) 传 id）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "agent:chat:memory:";
    /** 滑动窗口大小：保留最近 N 条消息（user+assistant 合计） */
    private static final int MAX_MESSAGES = 20;
    /** 会话过期时间 */
    private static final Duration TTL = Duration.ofDays(7);

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isBlank()
                || messages == null || messages.isEmpty()) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        List<String> serialized = new ArrayList<>();
        for (Message message : messages) {
            String json = serialize(message);
            if (json != null) {
                serialized.add(json);
            }
        }
        if (serialized.isEmpty()) {
            return;
        }
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] rawKey = key.getBytes();
                var listCommands = connection.listCommands();
                for (String json : serialized) {
                    listCommands.rPush(rawKey, json.getBytes());
                }
                // 滑动窗口：只保留最近 MAX_MESSAGES 条
                listCommands.lTrim(rawKey, -MAX_MESSAGES, -1);
                connection.keyCommands().expire(rawKey, TTL.toSeconds());
                return null;
            });
            log.debug("ChatMemory appended {} msgs for conversation {}", serialized.size(), conversationId);
        } catch (Exception e) {
            // 记忆写入失败不阻断对话主流程，仅记日志降级为无记忆
            log.error("ChatMemory write failed for {}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        try {
            List<String> rawList = redisTemplate.opsForList().range(KEY_PREFIX + conversationId, 0, -1);
            if (rawList == null || rawList.isEmpty()) {
                return List.of();
            }
            List<Message> messages = new ArrayList<>(rawList.size());
            for (String json : rawList) {
                Message message = deserialize(json);
                if (message != null) {
                    messages.add(message);
                }
            }
            log.debug("ChatMemory loaded {} msgs for conversation {}", messages.size(), conversationId);
            return messages;
        } catch (Exception e) {
            // 记忆读取失败降级为单轮对话，不阻断请求
            log.error("ChatMemory read failed for {}: {}", conversationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            redisTemplate.delete(KEY_PREFIX + conversationId);
            log.info("ChatMemory cleared for conversation {}", conversationId);
        }
    }

    /**
     * 只序列化纯文本 user/assistant 消息；其余类型（System/toolCalls/ToolResponse）返回 null 丢弃。
     */
    private String serialize(Message message) {
        try {
            MessageType type = message.getMessageType();
            String role;
            String content = message.getText();
            if (type == MessageType.USER) {
                role = ROLE_USER;
            } else if (type == MessageType.ASSISTANT && content != null && !content.isBlank()) {
                role = ROLE_ASSISTANT;
            } else {
                return null; // System / TOOL 中间消息不落库
            }
            return objectMapper.writeValueAsString(Map.of("role", role, "content", content));
        } catch (Exception e) {
            log.warn("ChatMemory serialize skipped one message: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Message deserialize(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            String role = String.valueOf(map.get("role"));
            String content = String.valueOf(map.get("content"));
            return ROLE_ASSISTANT.equals(role)
                    ? new AssistantMessage(content)
                    : new UserMessage(content);
        } catch (Exception e) {
            log.warn("ChatMemory deserialize skipped one entry: {}", e.getMessage());
            return null;
        }
    }
}
